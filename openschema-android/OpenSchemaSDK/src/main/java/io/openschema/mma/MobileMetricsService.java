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

package io.openschema.mma;

import android.app.ActivityManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkCapabilities;
import android.os.IBinder;
import android.util.Log;

import java.util.List;

import androidx.annotation.Nullable;
import androidx.core.util.Pair;
import io.openschema.mma.metrics.HourlyUsageWorker;
import io.openschema.mma.metrics.MetricsManager;
import io.openschema.mma.metrics.collectors.AsyncMetrics;
import io.openschema.mma.metrics.collectors.CellularSessionMetrics;
import io.openschema.mma.metrics.collectors.DeviceMetrics;
import io.openschema.mma.metrics.collectors.NetworkQualityMetrics;
import io.openschema.mma.metrics.collectors.WifiSessionMetrics;
import io.openschema.mma.utils.PersistentNotification;

/**
 * Foreground service used to keep the app running in the background and collect information.
 */
public class MobileMetricsService extends Service implements AsyncMetrics.MetricsCollectorListener {

    private static final String TAG = "MobileMetricsService";

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private MetricsManager mMetricsManager;
    private WifiSessionMetrics mWifiSessionMetrics;
    private CellularSessionMetrics mCellularSessionMetrics;
    private NetworkQualityMetrics mNetworkQualityMetrics;

    @Override
    public void onCreate() {
        Log.d(TAG, "MMA: Creating foreground service.");
        mMetricsManager = new MetricsManager(getApplicationContext());

        //Start listening to changes in wi-fi connections to measure duration and usage
        mWifiSessionMetrics = new WifiSessionMetrics(getApplicationContext(), this);
        mWifiSessionMetrics.startTrackers();

        //Start listening to changes in cellular connections to measure duration and usage
        mCellularSessionMetrics = new CellularSessionMetrics(getApplicationContext(), this);
        mCellularSessionMetrics.startTrackers();

        //Start listening to active network changes to measure quality
        mNetworkQualityMetrics = new NetworkQualityMetrics(getApplicationContext(), this, transportType -> {
            switch (transportType) {
                case NetworkCapabilities.TRANSPORT_WIFI:
                    return mWifiSessionMetrics.getCurrentConnectionId();
                case NetworkCapabilities.TRANSPORT_CELLULAR:
                    return mCellularSessionMetrics.getCurrentConnectionId();
                default:
                    return -1;
            }
        });
        mNetworkQualityMetrics.startTrackers();

        //Start periodic worker to measure network usage on a per hour basis
        HourlyUsageWorker.enqueuePeriodicWorker(getApplicationContext());

        //Collecting device metrics every time the service starts. Using this to let us know when the service might've stopped working on a device.
        mMetricsManager.collect(DeviceMetrics.METRIC_NAME, new DeviceMetrics(this).retrieveMetrics());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "MMA: Foreground service is starting.");

        PersistentNotification persistentNotification = PersistentNotification.getInstance(this);
        persistentNotification.show(getApplicationContext());
        startForeground(PersistentNotification.SERVICE_NOTIFICATION_ID, persistentNotification.getNotification());

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "MMA: Destroying foreground service.");
        mWifiSessionMetrics.stopTrackers();
        mCellularSessionMetrics.stopTrackers();
        mNetworkQualityMetrics.stopTrackers();
    }

    //Interface implementation to write asynchronous metrics to the DB queue to be pushed later.
    @Override
    public void onMetricCollected(String metricName, List<Pair<String, String>> metricsList) {
        mMetricsManager.collect(metricName, metricsList);
    }

    //Utility method to check if this service is running.
    @SuppressWarnings("deprecation")
    public static boolean isServiceRunning(Context context) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (MobileMetricsService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
}
