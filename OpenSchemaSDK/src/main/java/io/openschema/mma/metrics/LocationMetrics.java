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

package io.openschema.mma.metrics;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.util.Log;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.util.ArrayList;
import java.util.List;

import androidx.core.content.ContextCompat;
import androidx.core.util.Pair;

/**
 * Collects metrics related to device's location.
 */
public class LocationMetrics extends BaseMetrics {
    private static final String TAG = "LocationMetrics";

    /**
     * Metric name to be used for the collected information.
     */
    public static final String METRIC_NAME = "openschemaLocationInfo";

    private static final String METRIC_LATITUDE = "latitude";
    private static final String METRIC_LONGITUDE = "longitude";

    private final FusedLocationProviderClient mLocationClient;
    private final boolean mLocationPermissionGranted;

    private final MetricsCollectorListener mListener;

    public LocationMetrics(Context context, MetricsCollectorListener listener) {
        super(context);
        mListener = listener;

        mLocationClient = LocationServices.getFusedLocationProviderClient(context);

        mLocationPermissionGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    @SuppressLint("MissingPermission")
    public void requestLocation() {
        Log.d(TAG, "MMA: Generating location metrics...");
        //TODO: also check if location settings are enabled
        //TODO: need to evaluate the correct priority/accuracy
        //TODO: need to consider cases where google play services aren't available
        if (mLocationPermissionGranted) {
            mLocationClient.getCurrentLocation(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY, null).addOnSuccessListener(location -> {
                if (location != null) {
                    Log.d(TAG, "MMA: Location received successfully");
                } else {
                    Log.d(TAG, "MMA: Failed to compute location");
                }
                mListener.onMetricCollected(METRIC_NAME, extractLocationValues(location));
            }).addOnFailureListener(e -> {
                Log.d(TAG, "MMA: Failed to retrieve location");
                e.printStackTrace();
                mListener.onMetricCollected(METRIC_NAME, null);
            });
        }
    }

    private List<Pair<String, String>> extractLocationValues(Location location) {
        List<Pair<String, String>> metricsList = new ArrayList<>();

        if (location != null) {
            metricsList.add(new Pair<>(METRIC_LATITUDE, Double.toString(location.getLatitude())));
            metricsList.add(new Pair<>(METRIC_LONGITUDE, Double.toString(location.getLongitude())));
        } else {
            metricsList.add(new Pair<>(METRIC_LATITUDE, "null"));
            metricsList.add(new Pair<>(METRIC_LONGITUDE, "null"));
        }

        //TODO: Add debugging flag to enable detailed metrics
        Log.d(TAG, "MMA: Collected metrics:\n" + metricsList.toString());
        return metricsList;
    }

    public List<Pair<String, String>> retrieveMetrics() {return null; }
}
