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

import android.app.usage.NetworkStats;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.wifi.WifiInfo;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.core.util.Pair;
import io.openschema.mma.helpers.UsageRetriever;

/**
 * Collects metrics related to a Cellular session, tracked from connection until disconnection.
 */
//TODO: javadocs
public class CellularSessionMetrics extends BaseMetrics {
    private static final String TAG = "WifiSessionMetrics";

    /**
     * Metric family name to be used for the collected information.
     */
    public static final String METRIC_FAMILY_NAME = "openschema_android_cellular_session";

    private static final String METRIC_RX_BYTES = "rx_bytes";
    private static final String METRIC_TX_BYTES = "tx_bytes";
    private static final String METRIC_SESSION_DURATION_MILLIS = "session_duration_millis";

    private List<Pair<String, String>> mCurrentSession;
    private long mSessionStartTimestamp;

    private final ConnectivityManager mConnectivityManager;
    private final UsageRetriever mUsageRetriever;
    private final CellularNetworkMetrics mCellularNetworkMetrics;

    private final MetricsCollectorListener mListener;

    public CellularSessionMetrics(Context context, MetricsCollectorListener listener) {
        super(context);
        mListener = listener;

        mConnectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        mUsageRetriever = new UsageRetriever(context);
        mCellularNetworkMetrics = new CellularNetworkMetrics(context);
    }

    //TODO: The session needs to be split into hour-long segments

    private final ConnectivityManager.NetworkCallback mCellularCallBack = new ConnectivityManager.NetworkCallback() {
        @Override
        public void onAvailable(@NonNull Network network) {
            Log.d(TAG, "MMA: Detected Cellular connection");
            if (mCurrentSession != null) {
                //When we detect that a session was already in place, we'll still push it considering this instant as that session's end
                Log.d(TAG, "MMA: A session had been previously started.");
                calculateSessionStats();
                onMetricReady();
            }
            mSessionStartTimestamp = System.currentTimeMillis();
            mCurrentSession = mCellularNetworkMetrics.retrieveNetworkMetrics();
        }

        @Override
        public void onLost(@NonNull Network network) {
            Log.d(TAG, "MMA: Detected Cellular disconnection");
            calculateSessionStats();
            onMetricReady();
        }
    };

    private void calculateSessionStats() {
        long sessionEndTimestamp = System.currentTimeMillis();

        mCurrentSession.addAll(generateTimeZoneMetrics());

        long rxBytes = -1, txBytes = -1;

        //Increasing bucket window up to 1 hour forward so that we make sure to catch any recent bytes
        NetworkStats.Bucket wifiBucket = mUsageRetriever.getDeviceCellularBucket(mSessionStartTimestamp, sessionEndTimestamp + 60 * 60 * 1000);
        if (wifiBucket != null) {
            rxBytes = wifiBucket.getRxBytes();
            txBytes = wifiBucket.getTxBytes();
        }

        mCurrentSession.add(new Pair<>(METRIC_RX_BYTES, Long.toString(rxBytes)));
        mCurrentSession.add(new Pair<>(METRIC_TX_BYTES, Long.toString(txBytes)));

        long sessionDuration = sessionEndTimestamp - mSessionStartTimestamp;
        mCurrentSession.add(new Pair<>(METRIC_SESSION_DURATION_MILLIS, Long.toString(sessionDuration)));
    }

    private void onMetricReady() {
        Log.d(TAG, "MMA: Collected metrics:\n" + mCurrentSession.toString());
        mListener.onMetricCollected(METRIC_FAMILY_NAME, mCurrentSession);
        mCurrentSession = null;
    }

    public void startTrackers() {
        NetworkRequest wifiRequest = new NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
                .build();

        mConnectivityManager.requestNetwork(wifiRequest, mCellularCallBack);
    }

    public void stopTrackers() {
        mConnectivityManager.unregisterNetworkCallback(mCellularCallBack);
    }

    public List<Pair<String, String>> retrieveMetrics() {
        return null;
    }
}
