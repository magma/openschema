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
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import androidx.core.util.Pair;
import io.openschema.mma.data.entity.CellularConnectionsEntity;
import io.openschema.mma.data.entity.NetworkConnectionsEntity;
import io.openschema.mma.data.entity.WifiConnectionsEntity;

/**
 * Class to collect information for a connection report.
 * We are treating connection reports as another metric to be collected.
 */
public class ConnectionReport extends BaseMetrics {
    private static final String TAG = "ConnectionReport";

    /**
     * Metric name to be used for the collected information.
     */
    public static final String METRIC_NAME = "openschemaConnectionReport";

    public static final String METRIC_REPORT_DESCRIPTION = "reportDescription";
    public static final String METRIC_TRANSPORT_TYPE = "transportType";

    //Internal static enums used in OpenSchema ETL
    private static final String TRANSPORT_WIFI = "wifi";
    private static final String TRANSPORT_CELLULAR = "cellular";

    private final NetworkConnectionsEntity mConnectionEntity;
    private final String mReportDescription;

    public ConnectionReport(Context context, NetworkConnectionsEntity connectionEntity, String reportDescription) {
        super(context);
        mConnectionEntity = connectionEntity;
        mReportDescription = reportDescription;
    }

    public List<Pair<String, String>> retrieveMetrics() {
        Log.d(TAG, "MMA: Generating connection report...");
        List<Pair<String, String>> metricsList = new ArrayList<>();

        //Extract information shared by both network types
        metricsList.add(new Pair<>(METRIC_REPORT_DESCRIPTION, mReportDescription));
        metricsList.add(new Pair<>(LocationMetrics.METRIC_LATITUDE, Double.toString(mConnectionEntity.getLatitude())));
        metricsList.add(new Pair<>(LocationMetrics.METRIC_LONGITUDE, Double.toString(mConnectionEntity.getLongitude())));
        metricsList.add(new Pair<>(NetworkSessionMetrics.METRIC_SESSION_START_TIME, Long.toString(mConnectionEntity.getTimestamp())));

        //Extract network specific information
        if (mConnectionEntity instanceof WifiConnectionsEntity) {
            WifiConnectionsEntity entity = (WifiConnectionsEntity) mConnectionEntity;
            metricsList.add(new Pair<>(METRIC_TRANSPORT_TYPE, TRANSPORT_WIFI));
            metricsList.add(new Pair<>(WifiNetworkMetrics.METRIC_SSID, entity.getSSID()));
            metricsList.add(new Pair<>(WifiNetworkMetrics.METRIC_BSSID, entity.getBSSID()));
        } else if (mConnectionEntity instanceof CellularConnectionsEntity) {
            CellularConnectionsEntity entity = (CellularConnectionsEntity) mConnectionEntity;
            metricsList.add(new Pair<>(METRIC_TRANSPORT_TYPE, TRANSPORT_CELLULAR));
            metricsList.add(new Pair<>(CellularNetworkMetrics.METRIC_NETWORK_TYPE, entity.getNetworkType()));
            metricsList.add(new Pair<>(CellularNetworkMetrics.METRIC_CELL_ID, Long.toString(entity.getCellIdentity())));
        }

        Log.d(TAG, "MMA: Collected report:\n" + metricsList.toString());
        return metricsList;
    }
}
