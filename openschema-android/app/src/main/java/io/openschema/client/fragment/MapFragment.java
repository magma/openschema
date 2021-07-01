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

package io.openschema.client.fragment;

import android.annotation.SuppressLint;
import android.content.Context;
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
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.clustering.Cluster;
import com.google.maps.android.clustering.ClusterItem;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.clustering.view.DefaultClusterRenderer;

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
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavBackStackEntry;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import io.openschema.mma.data.entity.CellularConnectionsEntity;
import io.openschema.mma.data.entity.NetworkConnectionsEntity;
import io.openschema.mma.data.entity.WifiConnectionsEntity;
import io.openschema.client.R;
import io.openschema.client.activity.MainActivity;
import io.openschema.client.databinding.FragmentMapBinding;
import io.openschema.client.util.FormattingUtils;
import io.openschema.client.view.ConnectionReportDialog;
import io.openschema.client.viewmodel.MapViewModel;
import io.openschema.mma.metrics.collectors.ConnectionReport;
import io.openschema.mma.utils.LocationServicesChecker;

public class MapFragment extends Fragment implements OnMapReadyCallback {

    private static final String TAG = "MapFragment";
    private FragmentMapBinding mBinding;
    private MapViewModel mViewModel;

    private GoogleMap mGoogleMap = null;

    //TODO: Persisted with ViewModel instead?
    private HashMap<String, NetworkConnectionsEntity> mSeenEntitiesMap = new HashMap<>();
    private CustomItem mCurrentSelection = null;

    private int mWifiHue, mCellularHue;
    private ClusterManager<CustomItem> mClusterManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        mBinding = FragmentMapBinding.inflate(inflater, container, false);
        mViewModel = new ViewModelProvider(this).get(MapViewModel.class);
        return mBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mBinding.mapReportConnectionBtn.setOnClickListener(v -> onConnectionReportOpened());

        mBinding.mapTimeSelector.setOnTimeWindowChangedListener(newWindow -> {
            mViewModel.setCurrentTimeWindow(newWindow);
            //Clear mSeenEntitiesMap since connections might be entirely different after window change
            mClusterManager.clearItems();
            mSeenEntitiesMap.clear();
        });

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
    //Ignoring location permission since it's mandatory for the app
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        Log.d(TAG, "UI: Map is ready");
        mGoogleMap = googleMap;
        mClusterManager = new ClusterManager<>(requireContext(), mGoogleMap);

        //Configure map object
        mGoogleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(requireContext(), R.raw.map_style));
        mGoogleMap.setOnCameraIdleListener(mClusterManager);
        mClusterManager.setRenderer(new CustomClusterRenderer(requireContext(), mGoogleMap, mClusterManager));

        //Cluster markers
        mClusterManager.setOnClusterClickListener(this::onClusterSelected);
        mClusterManager.getClusterMarkerCollection().setInfoWindowAdapter(new CustomClusterInfoWindowAdapter(getLayoutInflater(), mViewModel));
        //Connection markers
        mClusterManager.setOnClusterItemClickListener(this::onMarkerSelected);
        mClusterManager.getMarkerCollection().setInfoWindowAdapter(new CustomItemInfoWindowAdapter(getLayoutInflater()));

        mGoogleMap.setOnInfoWindowCloseListener(marker -> onMarkerDeselected());
        mGoogleMap.setMyLocationEnabled(true);
        mGoogleMap.getUiSettings().setMapToolbarEnabled(false);
        mGoogleMap.getUiSettings().setRotateGesturesEnabled(false);

        //Setup observer for network connections from SDK
        mViewModel.getConnectionEntities().observe(getViewLifecycleOwner(), networkConnectionsEntities -> {
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
        mClusterManager.cluster();
    }

    //Create values to configure the data contained in each marker
    private void createMarker(NetworkConnectionsEntity currentEntity) {

        float currentIconHue = currentEntity.getTransportType() == NetworkCapabilities.TRANSPORT_WIFI ? mWifiHue : mCellularHue;

        CustomItem newItem = new CustomItem(currentEntity, BitmapDescriptorFactory.defaultMarker(currentIconHue));
        mClusterManager.addItem(newItem);

        //Save the db entry's ID in the marker for reference
        mSeenEntitiesMap.put(currentEntity.getCompoundId(), currentEntity);
    }

    private boolean onMarkerSelected(CustomItem item) {
        //Retrieve the db entry's ID
        NetworkConnectionsEntity entity = mSeenEntitiesMap.get(item.getId());

        //Check that the marker hasn't been reported already and show the reporting button.
        if (entity != null && !entity.getIsReported()) {
            mBinding.mapReportConnection.setVisibility(View.VISIBLE);
            mCurrentSelection = item;
        }
        return false;
    }

    //Clear the cached selected marker ID and hide the connection report button.
    private void onMarkerDeselected() {
        Log.d(TAG, "UI: onMarkerDeselected");
        //Need to check if there is a selection since this callback occurs on either a Cluster or ClusterItem.
        if (mCurrentSelection != null) {
            Log.d(TAG, "UI: onMarkerDeselected: " + mCurrentSelection.getId());
            mBinding.mapReportConnection.setVisibility(View.GONE);
            mCurrentSelection = null;
        }

        mViewModel.setSelectedClusterData(null);
    }

    //Opens a report dialog and handles the result
    private void onConnectionReportOpened() {
        if (mCurrentSelection == null) {
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
        final NetworkConnectionsEntity connectionEntity = mSeenEntitiesMap.get(mCurrentSelection.getId());
        final ConnectionReport connectionReport = new ConnectionReport(requireContext(), connectionEntity, reportDescription);

        //Collect metric into SDK's buffer.
        ((MainActivity) requireActivity()).pushMetric(ConnectionReport.METRIC_NAME, connectionReport.retrieveMetrics());

        //Mark the network connection locally as reported to prevent reporting the same connection multiple times.
        mViewModel.flagNetworkConnectionReported(connectionEntity);

        //Remove the marker from both the hashmap and map view for it to be updated.
        mSeenEntitiesMap.remove(mCurrentSelection.getId());
        mClusterManager.removeItem(mCurrentSelection);
    }

    //Calculate the cluster's aggregated values and store them to show in the info window
    private boolean onClusterSelected(Cluster<CustomItem> cluster) {
        List<CustomItem> clusterItems = (List<CustomItem>) cluster.getItems();
        long totalCellularUsage = 0, totalWifiUsage = 0;
        long totalCellularDuration = 0, totalWifiDuration = 0;

        for (int i = 0; i < clusterItems.size(); i++) {
            CustomItem currentItem = clusterItems.get(i);
            if (currentItem.getEntity().getTransportType() == NetworkCapabilities.TRANSPORT_CELLULAR) {
                totalCellularUsage += currentItem.getEntity().getUsage();
                totalCellularDuration += currentItem.getEntity().getDuration();
            } else if (currentItem.getEntity().getTransportType() == NetworkCapabilities.TRANSPORT_WIFI) {
                totalWifiUsage += currentItem.getEntity().getUsage();
                totalWifiDuration += currentItem.getEntity().getDuration();
            }
        }

        mViewModel.setSelectedClusterData(new ClusterData(cluster.getSize(), totalCellularUsage, totalCellularDuration, totalWifiUsage, totalWifiDuration));
        return false;
    }

    //Class to handle customized view when opening a marker within the map.
    private static class CustomItemInfoWindowAdapter implements GoogleMap.InfoWindowAdapter {

        private final View mContents;

        CustomItemInfoWindowAdapter(LayoutInflater layoutInflater) {
            mContents = layoutInflater.inflate(R.layout.view_custom_item_info_window, null);
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

    //Class to handle customized view when opening a marker within the map.
    private static class CustomClusterInfoWindowAdapter implements GoogleMap.InfoWindowAdapter {

        private final View mContents;
        private MapViewModel mViewModel;

        CustomClusterInfoWindowAdapter(LayoutInflater layoutInflater, MapViewModel viewModel) {
            mContents = layoutInflater.inflate(R.layout.view_custom_cluster_info_window, null);
            mViewModel = viewModel;
        }

        @Nullable
        @Override
        public View getInfoWindow(@NonNull Marker marker) {
            return null;
        }
        @Nullable
        @Override
        public View getInfoContents(@NonNull Marker marker) {
            ClusterData clusterData = mViewModel.getSelectedClusterData();
            TextView titleTxt = mContents.findViewById(R.id.info_title);
            titleTxt.setText(String.format("%d Connections", clusterData.getItemCount()));

            TextView cellularUsageTxt = mContents.findViewById(R.id.info_cellular_usage);
            cellularUsageTxt.setText(String.format("Cellular Usage: %s", FormattingUtils.humanReadableByteCountSI(clusterData.getTotalCellularUsage())));

            TextView cellularDurationTxt = mContents.findViewById(R.id.info_cellular_duration);
            cellularDurationTxt.setText(String.format("Cellular Duration: %s", FormattingUtils.humanReadableTime(clusterData.getTotalCellularDuration())));

            TextView wifiUsageTxt = mContents.findViewById(R.id.info_wifi_usage);
            wifiUsageTxt.setText(String.format("Wi-Fi Usage: %s", FormattingUtils.humanReadableByteCountSI(clusterData.getTotalWifiUsage())));

            TextView wifiDurationTxt = mContents.findViewById(R.id.info_wifi_duration);
            wifiDurationTxt.setText(String.format("Wi-Fi Duration: %s", FormattingUtils.humanReadableTime(clusterData.getTotalWifiDuration())));

            return mContents;
        }
    }

    //POJO to handle a cluster's information for it's custom info window
    public static class ClusterData {
        private final long mItemCount;
        private final long mTotalCellularUsage;
        private final long mTotalCellularDuration;
        private final long mTotalWifiUsage;
        private final long mTotalWifiDuration;

        public ClusterData(int itemCount, long totalCellularUsage, long totalCellularDuration, long totalWifiUsage, long totalWifiDuration) {
            mItemCount = itemCount;
            mTotalCellularUsage = totalCellularUsage;
            mTotalCellularDuration = totalCellularDuration;
            mTotalWifiUsage = totalWifiUsage;
            mTotalWifiDuration = totalWifiDuration;
        }

        public long getItemCount() { return mItemCount; }
        public long getTotalCellularUsage() { return mTotalCellularUsage; }
        public long getTotalCellularDuration() { return mTotalCellularDuration; }
        public long getTotalWifiUsage() { return mTotalWifiUsage; }
        public long getTotalWifiDuration() { return mTotalWifiDuration; }

    }

    //Class to handle clustered markers on the map
    private static class CustomItem implements ClusterItem {
        private final NetworkConnectionsEntity mEntity;
        private final LatLng mPosition;
        private final String mTitle;
        private final String mSnippet;
        private final BitmapDescriptor mIcon;

        public CustomItem(NetworkConnectionsEntity networkConnectionsEntity, BitmapDescriptor icon) {
            //Each data point is split by a newline to be able to process it later on the custom info window
            StringBuilder snippetBuilder = new StringBuilder();
            snippetBuilder.append(DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(new Date(networkConnectionsEntity.getTimestamp())));
            snippetBuilder.append("\n").append("Duration: ").append(FormattingUtils.humanReadableTime(networkConnectionsEntity.getDuration()));
            snippetBuilder.append("\n").append("Usage: ").append(FormattingUtils.humanReadableByteCountSI(networkConnectionsEntity.getUsage()));
            if (networkConnectionsEntity.getIsReported()) {
                snippetBuilder.append("\n").append("You reported this connection.");
            }

            //Check the network type for the connection entity and extract network-specific information
            String title;
            if (networkConnectionsEntity instanceof WifiConnectionsEntity) {
                WifiConnectionsEntity newEntity = (WifiConnectionsEntity) networkConnectionsEntity;
                title = String.format("Wi-Fi: %s", newEntity.getSSID());
            } else if (networkConnectionsEntity instanceof CellularConnectionsEntity) {
                CellularConnectionsEntity newEntity = (CellularConnectionsEntity) networkConnectionsEntity;
                title = String.format("Cellular (%s): %s", newEntity.getNetworkType(), newEntity.getCellIdentity());
            } else {
                title = networkConnectionsEntity.getTransportType() == NetworkCapabilities.TRANSPORT_WIFI ? "Wi-Fi Connection" : "Cellular Connection";
            }

            mEntity = networkConnectionsEntity;
            mPosition = new LatLng(networkConnectionsEntity.getLatitude(), networkConnectionsEntity.getLongitude());
            mTitle = title;
            mSnippet = snippetBuilder.toString();
            mIcon = icon;
        }

        public NetworkConnectionsEntity getEntity() { return mEntity;}

        public String getId() { return mEntity.getCompoundId(); }

        @Override
        public LatLng getPosition() { return mPosition; }

        @Override
        public String getTitle() { return mTitle; }

        @Override
        public String getSnippet() { return mSnippet; }

        public BitmapDescriptor getIcon() { return mIcon; }
    }

    //Class to handle custom markers on the map when using the clustering classes
    private static class CustomClusterRenderer extends DefaultClusterRenderer<CustomItem> {

        public CustomClusterRenderer(Context context, GoogleMap map, ClusterManager<CustomItem> clusterManager) {
            super(context, map, clusterManager);
        }

        @Override
        protected void onBeforeClusterItemRendered(@NonNull CustomItem item, @NonNull MarkerOptions markerOptions) {
            markerOptions
                    .position(item.getPosition())
                    .title(item.getTitle())
                    .snippet(item.getSnippet())
                    .icon(item.getIcon());
        }

        @Override
        protected void onClusterItemUpdated(@NonNull CustomItem item, @NonNull Marker marker) {
            marker.setPosition(item.getPosition());
            marker.setTitle(item.getTitle());
            marker.setSnippet(item.getSnippet());
            marker.setIcon(item.getIcon());
        }
    }
}