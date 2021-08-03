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

package io.openschema.mma.metrics.collectors;

import android.content.Context;
import android.location.Location;
import android.net.NetworkCapabilities;
import android.util.Log;

import io.openschema.mma.data.entity.NetworkConnectionsEntity;
import io.openschema.mma.data.entity.WifiConnectionsEntity;

/**
 * Collect metrics related to Wi-Fi sessions.
 */
public class WifiSessionMetrics extends NetworkSessionMetrics {
    private static final String TAG = "WifiSessionMetrics";

    /**
     * Metric name to be used for the collected information.
     */
    public static final String METRIC_NAME = "openschemaWifiSession";

    public WifiSessionMetrics(Context context, MetricsCollectorListener listener) {
        super(context, METRIC_NAME, NetworkCapabilities.TRANSPORT_WIFI, new WifiNetworkMetrics(context), listener);
        setNetworkConnectionEntityAdapter(new WifiConnectionAdapter());
    }

    class WifiConnectionAdapter implements NetworkConnectionEntityAdapter {

        @Override
        public NetworkConnectionsEntity getEntity() {
            Location lastLocation = mLocationMetrics.getLastLocation();
            //Initializing at MAX_VALUE since longitude ranges from -180 to 180 and latitude from -90 to 90
            double longitude = Double.MAX_VALUE, latitude = Double.MAX_VALUE;

            if (lastLocation != null) {
                Log.d(TAG, "MMA: Location was ready, creating entry with correct values. (transport: " + mTransportType + ")");
                longitude = lastLocation.getLongitude();
                latitude = lastLocation.getLatitude();
            }

            WifiNetworkMetrics wifiNetworkMetrics = (WifiNetworkMetrics) mNetworkMetrics;
            return new WifiConnectionsEntity(mTransportType, wifiNetworkMetrics.getSSID(), wifiNetworkMetrics.getBSSID(), longitude, latitude, mSessionStartTimestamp);
        }
    }


}