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

package io.openschema.mma.example.fragment;

import android.annotation.SuppressLint;
import android.net.NetworkCapabilities;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
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
import java.util.HashMap;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleEventObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.navigation.NavBackStackEntry;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import io.openschema.mma.data.MetricsRepository;
import io.openschema.mma.data.entity.CellularConnectionsEntity;
import io.openschema.mma.data.entity.NetworkConnectionsEntity;
import io.openschema.mma.data.entity.WifiConnectionsEntity;
import io.openschema.mma.example.activity.MainActivity;
import io.openschema.mma.example.R;
import io.openschema.mma.example.util.FormattingUtils;
import io.openschema.mma.example.databinding.FragmentMapBinding;
import io.openschema.mma.example.view.ConnectionReportDialog;
import io.openschema.mma.helpers.LocationServicesChecker;
import io.openschema.mma.metrics.collectors.ConnectionReport;

public class MapFragment extends Fragment implements OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

    private static final String TAG = "MapFragment";
    private FragmentMapBinding mBinding;
    private GoogleMap mGoogleMap = null;
    private MetricsRepository mMetricsRepository;

    //TODO: Persisted with ViewModel instead?
    private HashMap<String, NetworkConnectionsEntity> mSeenEntitiesMap = new HashMap<>();
    private Marker mCurrentMarker = null;

    private int mWifiHue, mCellularHue;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        mBinding = FragmentMapBinding.inflate(inflater, container, false);
        mMetricsRepository = MetricsRepository.getRepository(requireContext().getApplicationContext());
        return mBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mBinding.mapReportConnectionBtn.setOnClickListener(v -> onConnectionReportOpened());

        float[] hsl = new float[3];
        ColorUtils.colorToHSL(ContextCompat.getColor(requireContext(), R.color.wifiColor), hsl);
        mWifiHue = (int) hsl[0];
        ColorUtils.colorToHSL(ContextCompat.getColor(requireContext(), R.color.cellularColor), hsl);
        mCellularHue = (int) hsl[0];


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
        mGoogleMap.setOnInfoWindowCloseListener(marker -> onMarkerDeselected());
        mGoogleMap.setInfoWindowAdapter(new CustomInfoWindowAdapter());

        //Ignoring location permission since it's mandatory for the app
        mGoogleMap.setMyLocationEnabled(true);
        mGoogleMap.getUiSettings().setMapToolbarEnabled(false);
        mGoogleMap.getUiSettings().setRotateGesturesEnabled(false);

        //TODO: persist state in cache with a viewmodel?
        //Setup observer for network connections from SDK
        mMetricsRepository.getAllNetworkConnections().observe(getViewLifecycleOwner(), networkConnectionsEntities -> {
            if (networkConnectionsEntities != null) {
                onNetworkConnectionsReceived(networkConnectionsEntities);
            }
        });

        //Center camera on the device's current location and zoom to street level. If location services are not enabled, the camera will be centered around the last connection made.
        if (LocationServicesChecker.isLocationEnabled(requireContext())) {
            FusedLocationProviderClient locationClient = LocationServices.getFusedLocationProviderClient(requireContext());
            locationClient.getLastLocation().addOnSuccessListener(location -> {
                if (location != null) {
                    mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(), location.getLongitude()), 16));
                }
            });
        }

    }

    //Iterates through all the network connections received from observing the Room database and creates a marker in the google map instance for each unique session.
    private void onNetworkConnectionsReceived(List<NetworkConnectionsEntity> networkConnectionsEntities) {
        Log.d(TAG, "UI: There are " + networkConnectionsEntities.size() + " connections in DB");

        //Check that the map object was correctly initialized
        if (mGoogleMap == null) {
            Log.e(TAG, "UI: The google map hasn't loaded correctly.");
            return;
        }

        int processedCount = 0;

        //Iterate through all network connections
        for (int i = 0; i < networkConnectionsEntities.size(); i++) {
            NetworkConnectionsEntity currentEntity = networkConnectionsEntities.get(i);

            //Use a hashmap to cache connections that have been processed in previous observer events
            if (mSeenEntitiesMap.get(currentEntity.getCompoundId()) == null) {
                processedCount++;
                createMarker(currentEntity);
            }

            //Center camera around last marker and zoom to street level. This will only run if location services weren't enabled and the map couldn't be centered around the devices'current location.
            if (i == networkConnectionsEntities.size() - 1 && !LocationServicesChecker.isLocationEnabled(requireContext())) {
                mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(currentEntity.getLatitude(), currentEntity.getLongitude()), 16));
            }
        }

        Log.d(TAG, "UI: Added " + processedCount + " new markers to the map");
    }

    //Create values to configure the data contained in each marker
    private void createMarker(NetworkConnectionsEntity currentEntity) {
        LatLng currentLatLng = new LatLng(currentEntity.getLatitude(), currentEntity.getLongitude());
        float currentIconHue = currentEntity.getTransportType() == NetworkCapabilities.TRANSPORT_WIFI ? mWifiHue : mCellularHue;

        //Each data point is split by a newline to be able to process it later on the custom info window
        StringBuilder snippetBuilder = new StringBuilder();
        snippetBuilder.append(DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(new Date(currentEntity.getTimestamp())));
        snippetBuilder.append("\n").append("Duration: ").append(FormattingUtils.humanReadableTime(currentEntity.getDuration()));
        snippetBuilder.append("\n").append("Usage: ").append(FormattingUtils.humanReadableByteCountSI(currentEntity.getUsage()));
        if (currentEntity.getIsReported()) {
            snippetBuilder.append("\n").append("You reported this connection.");
        }

        //Check the network type for the connection entity and extract network-specific information
        String currentTitle;
        if (currentEntity instanceof WifiConnectionsEntity) {
            WifiConnectionsEntity newEntity = (WifiConnectionsEntity) currentEntity;
            currentTitle = String.format("Wi-Fi: %s", newEntity.getSSID());
        } else if (currentEntity instanceof CellularConnectionsEntity) {
            CellularConnectionsEntity newEntity = (CellularConnectionsEntity) currentEntity;
            currentTitle = String.format("Cellular (%s): %s", newEntity.getNetworkType(), newEntity.getCellIdentity());
        } else {
            currentTitle = currentEntity.getTransportType() == NetworkCapabilities.TRANSPORT_WIFI ? "Wi-Fi Connection" : "Cellular Connection";
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
            newMarker.setTag(currentEntity.getCompoundId());
            mSeenEntitiesMap.put(currentEntity.getCompoundId(), currentEntity);
        } else {
            Log.e(TAG, "UI: There was an error adding a marker to connection " + currentEntity.getId());
        }
    }

    @Override
    public boolean onMarkerClick(@NonNull Marker marker) {
        //Retrieve the db entry's ID
        NetworkConnectionsEntity entity = mSeenEntitiesMap.get(marker.getTag());

        //Check that the marker hasn't been reported already and show the reporting button.
        if (entity != null && !entity.getIsReported()) {
            mBinding.mapReportConnection.setVisibility(View.VISIBLE);
            mCurrentMarker = marker;
        }

        return false;
    }

    //Clear the cached selected marker ID and hide the connection report button.
    private void onMarkerDeselected() {
        mBinding.mapReportConnection.setVisibility(View.GONE);
        mCurrentMarker = null;
    }

    //Opens a report dialog and handles the result
    private void onConnectionReportOpened() {
        if (mCurrentMarker == null) {
            Log.e(TAG, "UI: No network connection has been selected");
            return;
        }

        //Open dialog for user to describe the issue with the connection.
        final NavController navController = NavHostFragment.findNavController(this);
        navController.navigate(R.id.action_nav_map_to_dialog_connection_report);

        //Setup observer to receive result when the fragment resumes after returning from the dialog.
        final NavBackStackEntry navBackStackEntry = navController.getBackStackEntry(R.id.nav_map);
        navBackStackEntry.getLifecycle().addObserver(new LifecycleEventObserver() {
            @Override
            public void onStateChanged(@NonNull LifecycleOwner source, @NonNull Lifecycle.Event event) {
                if (event.equals(Lifecycle.Event.ON_RESUME)) {
                    String reportDescription = navBackStackEntry.getSavedStateHandle().get(ConnectionReportDialog.KEY_REPORT_DESCRIPTION);
                    if (reportDescription != null) {
                        onConnectionReported(reportDescription);

                        //Remove value from SavedStateHandle to make sure we consume this event only once.
                        navBackStackEntry.getSavedStateHandle().remove(ConnectionReportDialog.KEY_REPORT_DESCRIPTION);
                    }

                    //Remove observer since event was resolved.
                    navBackStackEntry.getLifecycle().removeObserver(this);
                } else if (event.equals(Lifecycle.Event.ON_DESTROY)) {
                    //Remove observer in case the app was destroyed and the event wasn't resolved.
                    navBackStackEntry.getLifecycle().removeObserver(this);
                }
            }
        });
    }

    //Generate the connection report structure and use the SDK to collect it.
    private void onConnectionReported(String reportDescription) {
        final NetworkConnectionsEntity connectionEntity = mSeenEntitiesMap.get(mCurrentMarker.getTag());
        final ConnectionReport connectionReport = new ConnectionReport(requireContext(), connectionEntity, reportDescription);

        //Collect metric into SDK's buffer.
        ((MainActivity) requireActivity()).pushMetric(ConnectionReport.METRIC_NAME, connectionReport.retrieveMetrics());

        //Mark the network connection locally as reported to prevent reporting the same connection multiple times.
        mMetricsRepository.flagNetworkConnectionReported(connectionEntity);

        //Remove the marker from both the hashmap and map view for it to be updated.
        mSeenEntitiesMap.remove(mCurrentMarker.getTag());
        mCurrentMarker.remove();
    }

    //Class to handle customized view when opening a marker within the map.
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

            //Separate data separated by a newline that was prepared when creating the marker
            String[] markerContents = marker.getSnippet().split("\\r?\\n");

            TextView timestampTxt = mContents.findViewById(R.id.info_timestamp);
            timestampTxt.setText(markerContents[0]);

            TextView durationTxt = mContents.findViewById(R.id.info_duration);
            durationTxt.setText(markerContents[1]);

            TextView usageTxt = mContents.findViewById(R.id.info_usage);
            usageTxt.setText(markerContents[2]);

            TextView reportedTxt = mContents.findViewById(R.id.info_reported);
            if (markerContents.length == 4) {
                reportedTxt.setText(markerContents[3]);
                reportedTxt.setVisibility(View.VISIBLE);
            } else {
                reportedTxt.setVisibility(View.GONE);
            }

            return mContents;
        }
    }
}