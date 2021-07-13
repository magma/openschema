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
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkRequest;
import android.os.Handler;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.core.util.Pair;
import io.openschema.mma.data.MetricsRepository;
import io.openschema.mma.data.entity.NetworkConnectionsEntity;
import io.openschema.mma.data.entity.NetworkUsageEntity;
import io.openschema.mma.utils.UsageRetriever;

/**
 * Base class to collect metrics related to networks sessions, starting from connection to disconnection.
 */
public abstract class NetworkSessionMetrics extends BaseMetrics {
    private static final String TAG = "NetworkSessionMetrics";

    private final String METRIC_NAME;

    //Metric labels
    public static final String METRIC_RX_BYTES = "rxBytes";
    public static final String METRIC_TX_BYTES = "txBytes";
    public static final String METRIC_SESSION_START_TIME = "sessionStartTime";
    public static final String METRIC_SESSION_DURATION_MILLIS = "sessionDurationMillis";

    //Session data
    protected List<Pair<String, String>> mCurrentSession;
    protected long mSessionStartTimestamp, mSessionEndTimestamp;
    protected long mLastReportedSegmentTimestamp;
    protected boolean mIsExpectingLocation;
    protected long mLastRxBytes, mTotalRxBytes;
    protected long mLastTxBytes, mTotalTxBytes;
    protected long mTotalBytes;

    //Metrics sources
    protected final ConnectivityManager mConnectivityManager;
    protected final BaseMetrics mNetworkMetrics;
    protected final UsageRetriever mUsageRetriever;
    protected final LocationMetrics mLocationMetrics;

    private final MetricsCollectorListener mListener;
    protected final int mTransportType; //TODO: can cached data get lost if OS kills app temporarily?
    private NetworkConnectionEntityAdapter mNetworkConnectionEntityAdapter = null;

    protected Handler mHandler;
    private static final long FREQUENCE_BYTE_MEASUREMENT = 1000 * 60; //1 min
    private static final long FREQUENCE_SEGMENT_LOGGING = 1000 * 60 * 60; //60 min

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

        mHandler = new Handler();
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
        mLastReportedSegmentTimestamp = mSessionStartTimestamp;
        mCurrentSession = mNetworkMetrics.retrieveMetrics();
        mIsExpectingLocation = true;
        mLocationMetrics.requestLocation();
        //TODO: Persist the current session in SharedPrefs to avoid losing data in case app is killed temporarily.

        mLastRxBytes = mLastTxBytes = -1;
        mTotalRxBytes = mTotalTxBytes = mTotalBytes = 0;
        mHandler.post(mMeasureCurrentBytes);
        mHandler.postDelayed(mFlushSessionSegment, FREQUENCE_SEGMENT_LOGGING);
    }

    //Called when a network disconnection is detected.
    // May also be called if a new connection is detected without having detected the previous session's disconnection.
    protected void onSessionEnd() {
        mSessionEndTimestamp = System.currentTimeMillis();

        storeNetworkConnection();

        processSessionSegment(mLastReportedSegmentTimestamp, mSessionEndTimestamp);

        //Reset session-tracking variables
        mCurrentSession = null;
        mSessionStartTimestamp = -1;
        mSessionEndTimestamp = -1;
        mLastReportedSegmentTimestamp = -1;
        if (mIsExpectingLocation) {
            //TODO: We should monitor the data reaching the data lake. If too many instances are missing location information,
            //maybe we should consider waiting for the request to complete rather than ignoring.
            mLocationMetrics.cancelLocationRequest();
            mIsExpectingLocation = false;
        }
        mHandler.removeCallbacks(mMeasureCurrentBytes);
        mHandler.removeCallbacks(mFlushSessionSegment);
    }

    private final Runnable mMeasureCurrentBytes = new Runnable() {
        @Override
        public void run() {
            long newRxBytes = mUsageRetriever.getRxBytes(mTransportType);
            long newTxBytes = mUsageRetriever.getTxBytes(mTransportType);

            //Measure bytes received since last call
            if (mLastRxBytes != -1) {
                if (mLastRxBytes <= newRxBytes) {
                    long diff = newRxBytes - mLastRxBytes;
                    mTotalRxBytes += diff;
                    mTotalBytes += diff;
                } else {
                    //TODO: Apparently counter can hold a big enough value (uint64_t) that it might not reset.
                    Log.e(TAG, "MMA: Internal RxBytes counter was reset");
                }
            }

            //Measure bytes transmitted since last call
            if (mLastTxBytes != -1) {
                if (mLastTxBytes <= newTxBytes) {
                    long diff = newTxBytes - mLastTxBytes;
                    mTotalTxBytes += diff;
                    mTotalBytes += diff;
                } else {
                    Log.e(TAG, "MMA: Internal TxBytes counter was reset");
                }
            }

            //Save current value for next call
            mLastRxBytes = newRxBytes;
            mLastTxBytes = newTxBytes;

            //Run every 60 seconds
            mHandler.postDelayed(this, FREQUENCE_BYTE_MEASUREMENT);
        }
    };

    //Called when the LocationMetrics object finishes calculating the device's location.
    protected void onLocationReceived(List<Pair<String, String>> metricsList) {
        if (mIsExpectingLocation && metricsList != null) {
            Log.d(TAG, "MMA: Location received");
            mCurrentSession.addAll(metricsList);
            mIsExpectingLocation = false;
        }
    }

    protected void processSessionSegment(long segmentStart, long segmentEnd) {
//        Log.d(TAG, "MMA: Processing Window: " + segmentStart.getTime().toString() + " | " + segmentEnd.getTime().toString());

        //Create an independent metric list and copy the session's shared data.
        List<Pair<String, String>> currentSegmentMetrics = new ArrayList<>();
        currentSegmentMetrics.addAll(mCurrentSession);

        //Set window start time & duration in milliseconds.
        currentSegmentMetrics.add(new Pair<>(METRIC_SESSION_START_TIME, Long.toString(segmentStart)));
        long sessionDuration = segmentEnd - segmentStart;
        currentSegmentMetrics.add(new Pair<>(METRIC_SESSION_DURATION_MILLIS, Long.toString(sessionDuration)));

        //Set the received & transmitted bytes during this window.
        currentSegmentMetrics.add(new Pair<>(METRIC_RX_BYTES, Long.toString(mTotalRxBytes)));
        currentSegmentMetrics.add(new Pair<>(METRIC_TX_BYTES, Long.toString(mTotalTxBytes)));

        //TODO: Add debugging flag to enable detailed metrics
        Log.d(TAG, "MMA: Collected metrics:\n" + currentSegmentMetrics.toString());
        //Collect the metric locally to be pushed later.
        storeSessionSegment(sessionDuration, mTotalRxBytes + mTotalTxBytes, segmentStart);
        mListener.onMetricCollected(METRIC_NAME, currentSegmentMetrics);
    }

    //Save connection information into an optional local table to be used in UI
    protected void storeNetworkConnection() {
        //TODO: disable with flag from MMA builder? avoid extra calculations & storage
        if (mNetworkConnectionEntityAdapter != null) {
            mMetricsRepository.writeNetworkConnection(mNetworkConnectionEntityAdapter.getEntity());
        }
    }

    //Save usage information into an optional local table to be used in UI
    protected void storeSessionSegment(long duration, long usage, long timestamp) {
        //TODO: disable with flag from MMA builder? avoid extra calculations & storage
        mMetricsRepository.writeNetworkSessionSegment(new NetworkUsageEntity(mTransportType, duration, usage, timestamp));
    }

    private final Runnable mFlushSessionSegment = new Runnable() {
        @Override
        public void run() {
            //Check if connection exists
            if (mCurrentSession != null) {
                Log.d(TAG, "MMA: Flushing the networks' session information so far. (transport: " + mTransportType + ")");

                long currentTimestamp = System.currentTimeMillis();

                processSessionSegment(mLastReportedSegmentTimestamp, currentTimestamp);
                mTotalRxBytes = mTotalTxBytes = 0; //Reset counter for next segment
                mLastReportedSegmentTimestamp = currentTimestamp;

                mHandler.postDelayed(this, FREQUENCE_SEGMENT_LOGGING);
            }
        }
    };

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

    public void setNetworkConnectionEntityAdapter(NetworkConnectionEntityAdapter adapter) {
        mNetworkConnectionEntityAdapter = adapter;
    }

    public interface NetworkConnectionEntityAdapter {
        NetworkConnectionsEntity getEntity();
    }
}