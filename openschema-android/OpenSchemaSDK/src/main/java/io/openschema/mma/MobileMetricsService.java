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

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import java.util.List;

import androidx.annotation.Nullable;
import androidx.core.util.Pair;
import io.openschema.mma.utils.PersistentNotification;
import io.openschema.mma.metrics.collectors.BaseMetrics;
import io.openschema.mma.metrics.collectors.CellularSessionMetrics;
import io.openschema.mma.metrics.MetricsManager;
import io.openschema.mma.metrics.collectors.WifiSessionMetrics;

/**
 * Foreground service used to keep the app running in the background and collect information.
 */
public class MobileMetricsService extends Service implements BaseMetrics.MetricsCollectorListener {

    private static final String TAG = "MobileMetricsService";

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private MetricsManager mMetricsManager;
    private WifiSessionMetrics mWifiSessionMetrics;
    private CellularSessionMetrics mCellularSessionMetrics;

    @Override
    public void onCreate() {
        Log.d(TAG, "MMA: Creating foreground service.");
        mMetricsManager = new MetricsManager(getApplicationContext());

        mWifiSessionMetrics = new WifiSessionMetrics(getApplicationContext(), this);
        mWifiSessionMetrics.startTrackers();

        mCellularSessionMetrics = new CellularSessionMetrics(getApplicationContext(), this);
        mCellularSessionMetrics.startTrackers();
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
    }

    @Override
    public void onMetricCollected(String metricName, List<Pair<String, String>> metricsList) {
        mMetricsManager.collect(metricName, metricsList);
    }
}
