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
import android.util.Log;

import java.text.DateFormat;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import androidx.annotation.NonNull;
import androidx.core.util.Pair;
import io.openschema.mma.helpers.UsageRetriever;

/**
 * Collects metrics related to a Wi-Fi session, tracked from connection until disconnection.
 */
//TODO: javadocs
//TODO: Abstract into ConnectionSessionMetrics due to shared code with CellularSessionMetrics?
public class WifiSessionMetrics extends BaseMetrics {
    private static final String TAG = "WifiSessionMetrics";

    /**
     * Metric family name to be used for the collected information.
     */
    public static final String METRIC_FAMILY_NAME = "openschema_android_wifi_session";

    private static final String METRIC_RX_BYTES = "rx_bytes";
    private static final String METRIC_TX_BYTES = "tx_bytes";
    private static final String METRIC_SESSION_START_TIME_UTC = "session_start_time_utc";
    private static final String METRIC_SESSION_DURATION_MILLIS = "session_duration_millis";

    private List<Pair<String, String>> mCurrentSession;
    private long mSessionStartTimestamp, mSessionEndTimestamp;

    private final WifiNetworkMetrics mWifiNetworkMetrics;
    private final ConnectivityManager mConnectivityManager;
    private final UsageRetriever mUsageRetriever;

    private final MetricsCollectorListener mListener;

    public WifiSessionMetrics(Context context, MetricsCollectorListener listener) {
        super(context);
        mListener = listener;

        mWifiNetworkMetrics = new WifiNetworkMetrics(context);
        mConnectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        mUsageRetriever = new UsageRetriever(context);
    }

    //TODO: The session tracking might need persistence. (App killed temporarily and session is lost?)

    private final ConnectivityManager.NetworkCallback mWifiCallBack = new ConnectivityManager.NetworkCallback() {
        @Override
        public void onAvailable(@NonNull Network network) {
            Log.d(TAG, "MMA: Detected Wifi connection");
            if (mCurrentSession != null) {
                //When we detect that a session was already in place, we'll still push it considering this instant as that session's end
                Log.d(TAG, "MMA: A session had been previously started.");
                onSessionEnd();
            }
            mSessionStartTimestamp = System.currentTimeMillis();
            mCurrentSession = mWifiNetworkMetrics.retrieveNetworkMetrics();
        }

        @Override
        public void onLost(@NonNull Network network) {
            Log.d(TAG, "MMA: Detected Wifi disconnection");
            onSessionEnd();
        }
    };

    private void onSessionEnd() {
        mSessionEndTimestamp = System.currentTimeMillis();
        processConnectionSession();

        //Reset session-tracking variables
        mCurrentSession = null;
        mSessionStartTimestamp = -1;
        mSessionEndTimestamp = -1;
    }

    private void processConnectionSession() {
        //Setup the session's start & end time
        Calendar calculationStart = Calendar.getInstance();
        calculationStart.setTimeInMillis(mSessionStartTimestamp);
        Calendar calculationEnd = Calendar.getInstance();
        calculationEnd.setTimeInMillis(mSessionEndTimestamp);
        Log.d(TAG, "MMA: Session duration: " + calculationStart.getTime().toString() + " | " + calculationEnd.getTime().toString());

        //Get the hour starting this calculation
        calculationStart.set(Calendar.MINUTE, 0);
        calculationStart.set(Calendar.SECOND, 0);
        calculationStart.set(Calendar.MILLISECOND, 0);

        //Get the hour ending this calculation
        calculationEnd.set(Calendar.MINUTE, 0);
        calculationEnd.set(Calendar.SECOND, 0);
        calculationEnd.set(Calendar.MILLISECOND, 0);
        calculationEnd.add(Calendar.HOUR_OF_DAY, 1);

        //Split the whole session in hour-long segments
        long hoursDiff = ChronoUnit.HOURS.between(calculationStart.toInstant(), calculationEnd.toInstant());
        Log.d(TAG, "MMA: Hour windows included in this session: " + hoursDiff);
        Log.d(TAG, "MMA: Calculation window: " + calculationStart.getTime().toString() + " | " + calculationEnd.getTime().toString());

        Calendar currentSegmentStart = Calendar.getInstance();
        currentSegmentStart.setTimeInMillis(calculationStart.getTimeInMillis());
        for (int i = 0; i < hoursDiff; i++) {
            Calendar segmentStart = Calendar.getInstance();
            Calendar segmentEnd = Calendar.getInstance();

            if (i == 0) {
                //Set window start to session start instead
                segmentStart.setTimeInMillis(mSessionStartTimestamp);
            } else {
                segmentStart.setTimeInMillis(currentSegmentStart.getTimeInMillis());
            }

            if (i == hoursDiff - 1) {
                //Set window end to session end instead
                segmentEnd.setTimeInMillis(mSessionEndTimestamp);
            } else {
                segmentEnd.setTimeInMillis(segmentStart.getTimeInMillis());
                segmentEnd.set(Calendar.MINUTE, 0);
                segmentEnd.set(Calendar.SECOND, 0);
                segmentEnd.set(Calendar.MILLISECOND, 0);
                segmentEnd.add(Calendar.HOUR_OF_DAY, 1);
            }

            //Retrieve stats within this segment and push it
            processSessionSegment(segmentStart, segmentEnd);

            //Prepare next iteration
            currentSegmentStart.setTimeInMillis(segmentEnd.getTimeInMillis());
        }
    }

    private void processSessionSegment(Calendar segmentStart, Calendar segmentEnd) {
        Log.d(TAG, "MMA: Processing Window: " + segmentStart.getTime().toString() + " | " + segmentEnd.getTime().toString());

        List<Pair<String, String>> currentSegmentMetrics = new ArrayList<>();
        currentSegmentMetrics.addAll(mCurrentSession);
        currentSegmentMetrics.addAll(generateTimeZoneMetrics());

        //Set window start time in UTC & the duration in milliseconds
        DateFormat utcDateFormat = DateFormat.getTimeInstance();
        utcDateFormat.setTimeZone(TimeZone.getTimeZone("gmt"));
        currentSegmentMetrics.add(new Pair<>(METRIC_SESSION_START_TIME_UTC, utcDateFormat.format(new Date(segmentStart.getTimeInMillis()))));
        long sessionDuration = segmentEnd.getTimeInMillis() - segmentStart.getTimeInMillis();
        currentSegmentMetrics.add(new Pair<>(METRIC_SESSION_DURATION_MILLIS, Long.toString(sessionDuration)));

        //Set the received & transmitted bytes during this window
        long rxBytes = -1, txBytes = -1;
        NetworkStats.Bucket wifiBucket = mUsageRetriever.getDeviceWifiBucket(segmentStart.getTimeInMillis(), segmentEnd.getTimeInMillis()); //TODO: add additional time to bucket end? *ONLY FOR THE LAST SEGMENT* windowEnd.getTimeInMillis() + 60 * 60 * 1000
        if (wifiBucket != null) {
            rxBytes = wifiBucket.getRxBytes();
            txBytes = wifiBucket.getTxBytes();
        }
        currentSegmentMetrics.add(new Pair<>(METRIC_RX_BYTES, Long.toString(rxBytes)));
        currentSegmentMetrics.add(new Pair<>(METRIC_TX_BYTES, Long.toString(txBytes)));

        Log.d(TAG, "MMA: Collected metrics:\n" + currentSegmentMetrics.toString());
        mListener.onMetricCollected(METRIC_FAMILY_NAME, currentSegmentMetrics);
    }

    public void startTrackers() {
        NetworkRequest wifiRequest = new NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .build();

        mConnectivityManager.requestNetwork(wifiRequest, mWifiCallBack);
    }

    public void stopTrackers() {
        mConnectivityManager.unregisterNetworkCallback(mWifiCallBack);
    }


    public List<Pair<String, String>> retrieveMetrics() {
        return null;
    }
}
