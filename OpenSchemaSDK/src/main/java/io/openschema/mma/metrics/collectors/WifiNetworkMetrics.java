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
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import androidx.core.util.Pair;
import io.openschema.mma.metrics.MetricsManager;

/**
 * Collects metrics related to Wi-Fi networks.
 */
public class WifiNetworkMetrics extends BaseMetrics {

    private static final String TAG = "WifiNetworkMetrics";

    /**
     * Metric family name to be used for the collected Wi-Fi information.
     */
    public static final String METRIC_NAME = "openschemaWifiNetworkInfo";

    public static final String METRIC_SSID = "ssid";
    public static final String METRIC_BSSID = "bssid";

    private WifiManager mWifiManager;

    private String mSSID = null;
    private String mBSSID = null;

    public WifiNetworkMetrics(Context context) {
        super(context);
        mWifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
    }

    /**
     * Collects information about current Wi-Fi and generates a list of pairs to
     * be used in {@link MetricsManager#collect(String, List)}.
     */
    public List<Pair<String, String>> retrieveMetrics() {
        Log.d(TAG, "MMA: Generating Wi-Fi network metrics...");

        List<Pair<String, String>> metricsList = new ArrayList<>();
        WifiInfo wifiInfo = mWifiManager.getConnectionInfo();

        //TODO: check for location permission & service enabled
        mSSID = wifiInfo.getSSID().replaceAll("\"", "");
        String bssid = wifiInfo.getBSSID();
        mBSSID = bssid == null ? "null" : bssid;

        metricsList.add(new Pair<>(METRIC_SSID, mSSID));
        metricsList.add(new Pair<>(METRIC_BSSID, mBSSID));

//        Log.d(TAG, "MMA: Collected metrics:\n" + metricsList.toString());
        return metricsList;
    }

    //TODO: javadocs
    public String getSSID() {
        return mSSID;
    }

    public String getBSSID() {
        return mBSSID;
    }
}
