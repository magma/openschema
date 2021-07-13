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

import io.openschema.mma.data.entity.CellularConnectionsEntity;
import io.openschema.mma.data.entity.NetworkConnectionsEntity;
import io.openschema.mma.data.pojo.Timestamp;

/**
 * Collect metrics related to cellular sessions.
 */
public class CellularSessionMetrics extends NetworkSessionMetrics {

    /**
     * Metric name to be used for the collected information.
     */
    public static final String METRIC_NAME = "openschemaCellularSession";

    public CellularSessionMetrics(Context context, MetricsCollectorListener listener) {
        super(context, METRIC_NAME, NetworkCapabilities.TRANSPORT_CELLULAR, new CellularNetworkMetrics(context), listener);
        setNetworkConnectionEntityAdapter(new CellularConnectionAdapter());
    }

    class CellularConnectionAdapter implements NetworkConnectionEntityAdapter {

        @Override
        public NetworkConnectionsEntity getEntity() {
            Location lastLocation = mLocationMetrics.getLastLocation();
            if (lastLocation != null) {

                long sessionDuration = mSessionEndTimestamp - mSessionStartTimestamp;
                long sessionUsage = mTotalBytes;

                CellularNetworkMetrics cellularNetworkMetrics = (CellularNetworkMetrics) mNetworkMetrics;
                return new CellularConnectionsEntity(mTransportType, cellularNetworkMetrics.getNetworkType(), cellularNetworkMetrics.getCellIdentity(), sessionDuration, sessionUsage, lastLocation.getLongitude(), lastLocation.getLatitude(), mSessionStartTimestamp);
            }
            return null;
        }
    }
}