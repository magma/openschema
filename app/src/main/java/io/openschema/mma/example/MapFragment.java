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

import android.net.NetworkCapabilities;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.MarkerOptions;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import io.openschema.mma.data.MetricsRepository;
import io.openschema.mma.data.entity.NetworkConnectionsEntity;
import io.openschema.mma.example.databinding.FragmentMapBinding;

public class MapFragment extends Fragment implements OnMapReadyCallback {

    private static final String TAG = "MapFragment";
    private FragmentMapBinding mBinding;
    private GoogleMap mGoogleMap = null;

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

        //Start loading map
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(mBinding.mapContainer.getId());
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        Log.d(TAG, "UI: Map is ready");
        mGoogleMap = googleMap;

        //Configure map style
        mGoogleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(requireContext(), R.raw.map_style));

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

            //TODO: Extract MarkerOptions object configuration into a separate method
            //Configure values to draw the Marker
            LatLng currentLatLng = new LatLng(currentEntity.mLatitude, currentEntity.mLongitude);
            String currentTitle = currentEntity.mTransportType == NetworkCapabilities.TRANSPORT_WIFI ? "Wi-Fi Connection" : "Cellular Connection";
            float currentIconHue = currentEntity.mTransportType == NetworkCapabilities.TRANSPORT_WIFI ? BitmapDescriptorFactory.HUE_AZURE : BitmapDescriptorFactory.HUE_ORANGE;
            String currentSnippet = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(new Date(currentEntity.mTimeStamp.getTimestampMillis()));

            //Add the Marker to the map
            mGoogleMap.addMarker(new MarkerOptions()
                    .position(currentLatLng)
                    .title(currentTitle)
                    .snippet(currentSnippet)
                    .icon(BitmapDescriptorFactory.defaultMarker(currentIconHue))
            );

            //Center camera around last marker and zoom to street level
            if (i == networkConnectionsEntities.size() - 1) {
                mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 16));
            }
        }
    }
}