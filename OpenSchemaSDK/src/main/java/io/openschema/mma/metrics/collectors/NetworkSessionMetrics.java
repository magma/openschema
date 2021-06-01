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
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkRequest;
import android.util.Log;

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.core.util.Pair;
import io.openschema.mma.data.MetricsRepository;
import io.openschema.mma.data.entity.NetworkConnectionsEntity;
import io.openschema.mma.data.pojo.Timestamp;
import io.openschema.mma.helpers.UsageRetriever;

//TODO: javadocs
public abstract class NetworkSessionMetrics extends BaseMetrics {
    private static final String TAG = "NetworkSessionMetrics";

    private final String METRIC_NAME;

    //Metric labels
    public static final String METRIC_RX_BYTES = "rxBytes";
    public static final String METRIC_TX_BYTES = "txBytes";
    public static final String METRIC_SESSION_START_TIME = "sessionStartTime";
    public static final String METRIC_SESSION_DURATION_MILLIS = "sessionDurationMillis";

    //Session data
    private List<Pair<String, String>> mCurrentSession;
    private long mSessionStartTimestamp, mSessionEndTimestamp;
    private boolean mSessionLocationReceived;

    //Metrics sources
    private final ConnectivityManager mConnectivityManager;
    private final BaseMetrics mNetworkMetrics;
    private final UsageRetriever mUsageRetriever;
    private final LocationMetrics mLocationMetrics;

    private final MetricsCollectorListener mListener;
    private final int mTransportType; //TODO: data can get lost if OS kills app temporarily?

    private final MetricsRepository mMetricsRepository;

    public NetworkSessionMetrics(Context context, String metricName, int transportType, BaseMetrics networkMetrics, MetricsCollectorListener listener) {
        super(context);
        METRIC_NAME = metricName;
        mTransportType = transportType;
        mNetworkMetrics = networkMetrics;
        mListener = listener;
        mConnectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        mUsageRetriever = new UsageRetriever(context);
        mLocationMetrics = new LocationMetrics(context, (locationMetricName, metricsList) -> onLocationReceived(metricsList));
        mMetricsRepository = MetricsRepository.getRepository(context.getApplicationContext());
    }

    private final ConnectivityManager.NetworkCallback mNetworkCallBack = new ConnectivityManager.NetworkCallback() {
        @Override
        public void onAvailable(@NonNull Network network) {
            Log.d(TAG, "MMA: Detected network connection (transport: " + mTransportType + ")");
            if (mCurrentSession != null) {
                //When we detect that a session was already in place, we'll still push it considering this instant as that session's end
                Log.d(TAG, "MMA: A session had been previously started.");
                onSessionEnd();
            }
            onSessionStart();
        }

        @Override
        public void onLost(@NonNull Network network) {
            Log.d(TAG, "MMA: Detected network disconnection (transport: " + mTransportType + ")");
            onSessionEnd();
        }
    };

    //Called when a network connection is detected
    protected void onSessionStart() {
        mSessionStartTimestamp = System.currentTimeMillis();
        mCurrentSession = mNetworkMetrics.retrieveMetrics();
        mSessionLocationReceived = false;
        mLocationMetrics.requestLocation();
        //TODO: Persist the current session in SharedPrefs to avoid losing data in case app is killed temporarily.

    }

    //Called when a network disconnection is detected.
    // May also be called if a new connection is detected without having detected the previous session's disconnection.
    protected void onSessionEnd() {
        mSessionEndTimestamp = System.currentTimeMillis();
        //TODO: wait until session location is received? mSessionLocationReceived is currently ignored
        processConnectionSession();

        //Reset session-tracking variables
        mCurrentSession = null;
        mSessionStartTimestamp = -1;
        mSessionEndTimestamp = -1;
        mSessionLocationReceived = false;
    }

    //Called when the LocationMetrics object finishes calculating the device's location.
    protected void onLocationReceived(List<Pair<String, String>> metricsList) {
        mSessionLocationReceived = true;
        if (metricsList != null) {
            Log.d(TAG, "MMA: Location received");
            mCurrentSession.addAll(metricsList);

            //Save connection into an optional local table to be used in UI
            Location lastLocation = mLocationMetrics.getLastLocation();
            if (lastLocation != null) {
                mMetricsRepository.writeNetworkConnection(new NetworkConnectionsEntity(mTransportType, lastLocation.getLongitude(), lastLocation.getLatitude(), Timestamp.getTimestampInstance()));
            }
        }
    }

    //Retroactively calculate the time & tonnage spent on the network session. The session is split into hourly segments.
    //TODO: Provide example
    protected void processConnectionSession() {

        long hourSegments = getIterationHours();
        Log.d(TAG, "MMA: Hour windows included in this session: " + hourSegments);

        Calendar currentSegmentStart = Calendar.getInstance();
        currentSegmentStart.setTimeInMillis(mSessionStartTimestamp);
        for (int i = 0; i < hourSegments; i++) {
            Calendar segmentStart = Calendar.getInstance();
            Calendar segmentEnd = Calendar.getInstance();

            segmentStart.setTimeInMillis(currentSegmentStart.getTimeInMillis());

            //Check if we are currently on the last segment
            if (i == hourSegments - 1) {
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

            //Configure variables for next iteration
            currentSegmentStart.setTimeInMillis(segmentEnd.getTimeInMillis());
        }
    }

    //TODO: come up with a better term for "clock hours"
    //Calculate the amount of hour segments included in our session. The segments are based on clock hours.
    // (e.g. A session that ocurred at 4:37pm - 5:22pm would include 2 hour iterations, 4:00pm - 5:00pm and 5:00pm - 6:00pm.)
    protected long getIterationHours() {
        //Setup the session's start & end time
        //The "calculation" refers to the clock hours where the session took place.
        Calendar calculationStart = Calendar.getInstance();
        calculationStart.setTimeInMillis(mSessionStartTimestamp);
        Calendar calculationEnd = Calendar.getInstance();
        calculationEnd.setTimeInMillis(mSessionEndTimestamp);
        Log.d(TAG, "MMA: Session duration: " + calculationStart.getTime().toString() + " | " + calculationEnd.getTime().toString());

        //Get the clock hour starting this session
        calculationStart.set(Calendar.MINUTE, 0);
        calculationStart.set(Calendar.SECOND, 0);
        calculationStart.set(Calendar.MILLISECOND, 0);

        //Get the clock hour ending this session
        calculationEnd.set(Calendar.MINUTE, 0);
        calculationEnd.set(Calendar.SECOND, 0);
        calculationEnd.set(Calendar.MILLISECOND, 0);
        calculationEnd.add(Calendar.HOUR_OF_DAY, 1);

        //Calculate the amount of hour segments included
        Log.d(TAG, "MMA: Calculation window: " + calculationStart.getTime().toString() + " | " + calculationEnd.getTime().toString());
        return ChronoUnit.HOURS.between(calculationStart.toInstant(), calculationEnd.toInstant());
    }

    //Calculate the time and tonnage spent on the time segment. This will generate an independent metric to be collected & pushed to the data lake later.
    protected void processSessionSegment(Calendar segmentStart, Calendar segmentEnd) {
        Log.d(TAG, "MMA: Processing Window: " + segmentStart.getTime().toString() + " | " + segmentEnd.getTime().toString());

        //Create an independent metric list and copy the session's shared data.
        List<Pair<String, String>> currentSegmentMetrics = new ArrayList<>();
        currentSegmentMetrics.addAll(mCurrentSession);

        //Set window start time & duration in milliseconds.
        currentSegmentMetrics.add(new Pair<>(METRIC_SESSION_START_TIME, Long.toString(segmentStart.getTimeInMillis())));
        long sessionDuration = segmentEnd.getTimeInMillis() - segmentStart.getTimeInMillis();
        currentSegmentMetrics.add(new Pair<>(METRIC_SESSION_DURATION_MILLIS, Long.toString(sessionDuration)));

        //Set the received & transmitted bytes during this window.
        long rxBytes = -1, txBytes = -1;
        NetworkStats.Bucket networkBucket = mUsageRetriever.getDeviceNetworkBucket(mTransportType, segmentStart.getTimeInMillis(), segmentEnd.getTimeInMillis()); //TODO: add additional time to bucket end? *ONLY FOR THE LAST SEGMENT* windowEnd.getTimeInMillis() + 60 * 60 * 1000
        if (networkBucket != null) {
            rxBytes = networkBucket.getRxBytes();
            txBytes = networkBucket.getTxBytes();
        }
        currentSegmentMetrics.add(new Pair<>(METRIC_RX_BYTES, Long.toString(rxBytes)));
        currentSegmentMetrics.add(new Pair<>(METRIC_TX_BYTES, Long.toString(txBytes)));

        //TODO: Add debugging flag to enable detailed metrics
        Log.d(TAG, "MMA: Collected metrics:\n" + currentSegmentMetrics.toString());
        //Collect the metric locally to be pushed later.
        mListener.onMetricCollected(METRIC_NAME, currentSegmentMetrics);
    }

    //Starts tracking the network's changes
    public void startTrackers() {
        NetworkRequest wifiRequest = new NetworkRequest.Builder()
                .addTransportType(mTransportType)
                .build();

        mConnectivityManager.requestNetwork(wifiRequest, mNetworkCallBack);
    }

    //Stops tracking the network's changes
    public void stopTrackers() {
        mConnectivityManager.unregisterNetworkCallback(mNetworkCallBack);
    }


    public List<Pair<String, String>> retrieveMetrics() {
        return null;
    }
}