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

import android.app.usage.NetworkStats;
import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import androidx.core.util.Pair;
import io.openschema.mma.utils.TransportType;
import io.openschema.mma.utils.UsageRetriever;

/**
 * Wi-Fi and Cellular usage collected on an hourly basis.
 */
public class NetworkHourlyMetrics extends SyncMetrics {
    private static final String TAG = "WifiHourlyMetrics";

    /**
     * Metric name to be used for the collected information.
     */
    public static final String METRIC_NAME = "openschemaUsageHourly";

    public static final String METRIC_RX_BYTES = "rxBytes";
    public static final String METRIC_TX_BYTES = "txBytes";
    public static final String METRIC_SEGMENT_START_TIME = "segmentStartTime";

    private UsageRetriever mUsageRetriever;

    public NetworkHourlyMetrics(Context context) {
        super(context);
        mUsageRetriever = new UsageRetriever(context);
    }

    /**
     * Collects information about the tonnage used for the specified network during
     * the specified time window.
     */
    public List<Pair<String, String>> retrieveMetrics(int transportType, long startTime, long endTime) {
        Log.d(TAG, "MMA: Generating network hourly metrics...");

        List<Pair<String, String>> metricsList = new ArrayList<>();

        metricsList.add(new Pair<>(TransportType.METRIC_TRANSPORT_TYPE, TransportType.getTransportString(transportType)));

        NetworkStats.Bucket bucket = mUsageRetriever.getDeviceNetworkBucket(transportType, startTime, endTime);
        metricsList.add(new Pair<>(METRIC_RX_BYTES, Long.toString(bucket.getRxBytes())));
        metricsList.add(new Pair<>(METRIC_TX_BYTES, Long.toString(bucket.getTxBytes())));

        metricsList.add(new Pair<>(METRIC_SEGMENT_START_TIME, Long.toString(startTime)));

        //TODO: Add debugging flag to enable detailed metrics
        Log.d(TAG, "MMA: Collected metrics:\n" + metricsList.toString());
        return metricsList;
    }

    public List<Pair<String, String>> retrieveMetrics() { return null; }
}
