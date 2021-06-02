/*
 * Copyright (c) 2020, The Magma Authors
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.openschema.mma.example;

import android.annotation.SuppressLint;
import android.net.NetworkCapabilities;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import io.openschema.mma.data.MetricsRepository;
import io.openschema.mma.data.entity.CellularConnectionsEntity;
import io.openschema.mma.data.entity.NetworkConnectionsEntity;
import io.openschema.mma.data.entity.WifiConnectionsEntity;
import io.openschema.mma.example.databinding.FragmentMapBinding;

public class MapFragment extends Fragment implements OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

    private static final String TAG = "MapFragment";
    private FragmentMapBinding mBinding;
    private GoogleMap mGoogleMap = null;
    private int mCurrentMarkerId = -1;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        mBinding = FragmentMapBinding.inflate(inflater, container, false);
        return mBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mBinding.mapReportConnectionBtn.setOnClickListener(v -> onConnectionReported());

        //Start loading map
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(mBinding.mapContainer.getId());
        mapFragment.getMapAsync(this);
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        Log.d(TAG, "UI: Map is ready");
        mGoogleMap = googleMap;

        //Configure map object
        mGoogleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(requireContext(), R.raw.map_style));
        mGoogleMap.setOnMarkerClickListener(this);
        mGoogleMap.setOnInfoWindowCloseListener(marker -> onConnectionReportDismiss());
        mGoogleMap.setInfoWindowAdapter(new CustomInfoWindowAdapter());

        //TODO: check for location permission granted during runtime
        mGoogleMap.setMyLocationEnabled(true);
        mGoogleMap.getUiSettings().setMapToolbarEnabled(false);
        mGoogleMap.getUiSettings().setRotateGesturesEnabled(false);

        //TODO: persist state in cache with a viewmodel?
        //Setup observer for network connections from SDK
        MetricsRepository.getRepository(requireContext().getApplicationContext()).getAllNetworkConnections().observe(getViewLifecycleOwner(), networkConnectionsEntities -> {
            if (networkConnectionsEntities != null) {
                onNetworkConnectionsReceived(networkConnectionsEntities);
            }
        });

        //TODO: center map around current location. (If not available? Center around last connection's location?)
    }

    private void onNetworkConnectionsReceived(List<NetworkConnectionsEntity> networkConnectionsEntities) {
        Log.d(TAG, "UI: There are " + networkConnectionsEntities.size() + " connections in DB");

        //Check that the map object was correctly initialized
        if (mGoogleMap == null) {
            Log.e(TAG, "UI: The google map hasn't loaded correctly.");
            return;
        }

        //TODO: Avoid iterating through previously seen items since the whole list is going to be received during an update.
        // Maybe store the list locally and compare against it, similar to how RecyclerView handles it?
        //Iterate through all network connections
        for (int i = 0; i < networkConnectionsEntities.size(); i++) {
            NetworkConnectionsEntity currentEntity = networkConnectionsEntities.get(i);

            createMarker(currentEntity);

            //Center camera around last marker and zoom to street level
            if (i == networkConnectionsEntities.size() - 1) {
                mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(currentEntity.mLatitude, currentEntity.mLongitude), 16));
            }
        }
    }

    private void createMarker(NetworkConnectionsEntity currentEntity) {
        //Configure values to draw the Marker
        LatLng currentLatLng = new LatLng(currentEntity.mLatitude, currentEntity.mLongitude);
        float currentIconHue = currentEntity.mTransportType == NetworkCapabilities.TRANSPORT_WIFI ? BitmapDescriptorFactory.HUE_AZURE : BitmapDescriptorFactory.HUE_ORANGE;

        StringBuilder snippetBuilder = new StringBuilder();
        snippetBuilder.append(DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(new Date(currentEntity.mTimeStamp.getTimestampMillis())));
        snippetBuilder.append("\n").append("Duration: ").append(Utils.humanReadableTime(currentEntity.mDuration));
        snippetBuilder.append("\n").append("Usage: ").append(Utils.humanReadableByteCountSI(currentEntity.mUsage));

        String currentTitle;
        if (currentEntity instanceof WifiConnectionsEntity) {
            WifiConnectionsEntity newEntity = (WifiConnectionsEntity) currentEntity;
            currentTitle = String.format("Wi-Fi: %s", newEntity.mSSID);
        } else if (currentEntity instanceof CellularConnectionsEntity) {
            CellularConnectionsEntity newEntity = (CellularConnectionsEntity) currentEntity;
            currentTitle = String.format("Cellular (%s): %s", newEntity.mNetworkType, newEntity.mCellIdentity);
        } else {
            currentTitle = currentEntity.mTransportType == NetworkCapabilities.TRANSPORT_WIFI ? "Wi-Fi Connection" : "Cellular Connection";
        }

        //Add the Marker to the map
        Marker newMarker = mGoogleMap.addMarker(new MarkerOptions()
                .position(currentLatLng)
                .title(currentTitle)
                .snippet(snippetBuilder.toString())
                .icon(BitmapDescriptorFactory.defaultMarker(currentIconHue))
        );

        //Save the db entry's ID in the marker for reference
        if (newMarker != null) {
            newMarker.setTag(currentEntity.mId);
        } else {
            Log.e(TAG, "UI: There was an error adding a marker to connection " + currentEntity.mId);
        }
    }

    @Override
    public boolean onMarkerClick(@NonNull Marker marker) {
        //Retrieve the db entry's ID
        Integer networkConnectionId = (Integer) marker.getTag();

        Log.d(TAG, "UI: onMarkerClick: " + marker.getSnippet());

        if (networkConnectionId != null) {
            mBinding.mapReportConnection.setVisibility(View.VISIBLE);
            mCurrentMarkerId = networkConnectionId;
        } else {
            Log.e(TAG, "UI: The marker didn't have a correct ID attached");
        }

        return false;
    }

    private void onConnectionReported() {
        if (mCurrentMarkerId == -1) {
            Log.e(TAG, "UI: No network connection has been selected");
            return;
        }

        Log.d(TAG, "UI: Attempting to report connection with ID: " + mCurrentMarkerId);
    }

    private void onConnectionReportDismiss() {
        mBinding.mapReportConnection.setVisibility(View.GONE);
        mCurrentMarkerId = -1;
    }

    class CustomInfoWindowAdapter implements GoogleMap.InfoWindowAdapter {

        private final View mContents;

        CustomInfoWindowAdapter() {
            mContents = getLayoutInflater().inflate(R.layout.view_custom_info_window, null);
        }

        @Nullable
        @Override
        public View getInfoWindow(@NonNull Marker marker) {
            return null;
        }
        @Nullable
        @Override
        public View getInfoContents(@NonNull Marker marker) {

            TextView titleTxt = mContents.findViewById(R.id.info_title);
            titleTxt.setText(marker.getTitle());

            String[] markerContents = marker.getSnippet().split("\\r?\\n");

            TextView timestampTxt = mContents.findViewById(R.id.info_timestamp);
            timestampTxt.setText(markerContents[0]);

            TextView durationTxt = mContents.findViewById(R.id.info_duration);
            durationTxt.setText(markerContents[1]);

            TextView usageTxt = mContents.findViewById(R.id.info_usage);
            usageTxt.setText(markerContents[2]);

            return mContents;
        }
    }
}