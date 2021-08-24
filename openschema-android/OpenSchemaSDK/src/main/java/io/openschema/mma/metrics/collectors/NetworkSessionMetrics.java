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
public abstract class NetworkSessionMetrics extends AsyncMetrics {
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

    //Metrics sources
    protected final ConnectivityManager mConnectivityManager;
    protected final SyncMetrics mNetworkMetrics;
    protected final UsageRetriever mUsageRetriever;
    protected final LocationMetrics mLocationMetrics;

    private final MetricsCollectorListener mListener;
    protected final int mTransportType; //TODO: can cached data get lost if OS kills app temporarily?
    private NetworkConnectionEntityAdapter mNetworkConnectionEntityAdapter = null;

    protected Handler mHandler;
    private static final long FREQUENCE_BYTE_MEASUREMENT = 1000 * 15; //15 seconds
    private static final long FREQUENCE_SEGMENT_LOGGING = 1000 * 60 * 60; //60 min
    private static final long BYTES_THRESHOLD = 1000 * 1000 * 200; //200 MB

    private final MetricsRepository mMetricsRepository;
    private NetworkConnectionsEntity mCurrentActiveConnection = null;
    private NetworkUsageEntity mCurrentActiveSegment = null;

    public NetworkSessionMetrics(Context context, String metricName, int transportType, SyncMetrics networkMetrics, MetricsCollectorListener listener) {
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

        mLastRxBytes = -1;
        mLastTxBytes = -1;
        mTotalRxBytes = 0;
        mTotalTxBytes = 0;
        createNetworkConnection();
        mHandler.post(mMeasureCurrentBytes);
        mHandler.postDelayed(mFlushSessionSegment, FREQUENCE_SEGMENT_LOGGING);
    }

    //Called when a network disconnection is detected.
    // May also be called if a new connection is detected without having detected the previous session's disconnection.
    protected void onSessionEnd() {
        mSessionEndTimestamp = System.currentTimeMillis();

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
        mCurrentActiveConnection = null;
        mCurrentActiveSegment = null;
        mHandler.removeCallbacks(mMeasureCurrentBytes);
        mHandler.removeCallbacks(mFlushSessionSegment);
    }

    private final Runnable mMeasureCurrentBytes = new Runnable() {
        @Override
        public void run() {
            NetworkStats.Bucket newBucket = mUsageRetriever.getDeviceNetworkBucket(mTransportType, 0, System.currentTimeMillis());

            long totalBytesDiff = 0;

            if (mLastRxBytes != -1) {
                long diff = newBucket.getRxBytes() - mLastRxBytes;
                mTotalRxBytes += diff;
                totalBytesDiff += diff;
            }

            //Measure bytes transmitted since last call
            if (mLastTxBytes != -1) {
                long diff = newBucket.getTxBytes() - mLastTxBytes;
                mTotalTxBytes += diff;
                totalBytesDiff += diff;
            }

            //TODO: remove? used for accuracy testing purposes
            if (totalBytesDiff > BYTES_THRESHOLD || totalBytesDiff < 0) {
                Log.e(TAG, "MMA: The measurement caught an unusual amount over " + BYTES_THRESHOLD +
                        "Current usage measurements: (transport: " + mTransportType + ")" +
                        "\nDiff since last measurement (Total Bytes): " + totalBytesDiff +
                        "\nTotal Bytes: " + (mTotalRxBytes + mTotalTxBytes) +
                        "\nRx Bytes: " + mTotalRxBytes +
                        "\nTx Bytes: " + mTotalTxBytes);
            }

            //Save current value for next call
            mLastRxBytes = newBucket.getRxBytes();
            mLastTxBytes = newBucket.getTxBytes();

            long duration = System.currentTimeMillis() - mLastReportedSegmentTimestamp;
            updateSessionSegment(duration, mTotalRxBytes + mTotalTxBytes);

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

            //Connection was written to DB first, need to update with location
            if (mCurrentActiveConnection != null) {
                Log.d(TAG, "MMA: Network connection already existing, updating location values. (transport: " + mTransportType + ")");
                updateNetworkConnection(mLocationMetrics.getLastLocation());
            }
        }
    }

    protected void processSessionSegment(long segmentStart, long segmentEnd) {
//        Log.d(TAG, "MMA: Processing Window: " + segmentStart.getTime().toString() + " | " + segmentEnd.getTime().toString());

        //Create an independent metric list and copy the session's shared data.
        List<Pair<String, String>> currentSegmentMetrics = new ArrayList<>();
        currentSegmentMetrics.addAll(mCurrentSession);

        //Set window start time & duration in milliseconds.
        currentSegmentMetrics.add(new Pair<>(METRIC_SESSION_START_TIME, Long.toString(segmentStart)));
        long segmentDuration = segmentEnd - segmentStart;
        currentSegmentMetrics.add(new Pair<>(METRIC_SESSION_DURATION_MILLIS, Long.toString(segmentDuration)));

        //Set the received & transmitted bytes during this window.
        currentSegmentMetrics.add(new Pair<>(METRIC_RX_BYTES, Long.toString(mTotalRxBytes)));
        currentSegmentMetrics.add(new Pair<>(METRIC_TX_BYTES, Long.toString(mTotalTxBytes)));
        long segmentUsage = mTotalRxBytes + mTotalTxBytes;

        //TODO: Add debugging flag to enable detailed metrics
        Log.d(TAG, "MMA: Collected metrics:\n" + currentSegmentMetrics.toString());
        Log.d(TAG, "MMA: Current segment measurements: (transport: " + mTransportType + ")" +
                "\nSegment Duration: " + segmentDuration +
                "\nTotal Bytes: " + (mTotalRxBytes + mTotalTxBytes) +
                "\nSegment Total: " + segmentUsage +
                "\nRx Bytes: " + mTotalRxBytes +
                "\nTx Bytes: " + mTotalTxBytes);

        //Update segment entry with final values
        updateSessionSegment(segmentDuration, segmentUsage);

        //Collect the metric locally to be pushed later.
        mListener.onMetricCollected(METRIC_NAME, currentSegmentMetrics);
    }

    //Save connection information into an optional local table to be used in UI
    protected void createNetworkConnection() {
        //TODO: disable with flag from MMA builder? avoid extra calculations & storage
        Log.d(TAG, "MMA: Creating new database entry for network connection. (transport: " + mTransportType + ")");
        mCurrentActiveConnection = null;
        if (mNetworkConnectionEntityAdapter != null) {
            mMetricsRepository.writeNetworkConnection(mNetworkConnectionEntityAdapter.getEntity())
                    .thenAccept(entity -> {
                        mCurrentActiveConnection = entity;
                        createSessionSegment(mLastReportedSegmentTimestamp);
                    });
        }
    }

    //Updates the location values of the current connection's entry.
    protected void updateNetworkConnection(Location location) {
        //TODO: disable with flag from MMA builder? avoid extra calculations & storage
        if (mCurrentActiveConnection != null) {
            Log.d(TAG, "MMA: Updating database entry for network connection. (transport: " + mTransportType + ")");
            mCurrentActiveConnection.setLocation(location);
            mMetricsRepository.updateNetworkConnection(mCurrentActiveConnection);
        }
    }

    //Creates an initial database entry to be updated over the duration of a segment. This is an optional local table to be used in UI.
    protected void createSessionSegment(long timestamp) {
        //TODO: disable with flag from MMA builder? avoid extra calculations & storage
        Log.d(TAG, "MMA: Creating new database entry for network usage segment. (transport: " + mTransportType + ")");
        mCurrentActiveSegment = null;
        mMetricsRepository.writeNetworkSessionSegment(new NetworkUsageEntity(mCurrentActiveConnection.getId(), mTransportType, 0, 0, timestamp))
                .thenAccept(networkUsageEntity -> mCurrentActiveSegment = networkUsageEntity);
    }

    //Updates the duration and usage values of the current segment's entry.
    protected void updateSessionSegment(long duration, long usage) {
        //TODO: disable with flag from MMA builder? avoid extra calculations & storage
        if (mCurrentActiveSegment != null) {
            mCurrentActiveSegment.setDuration(duration);
            mCurrentActiveSegment.setUsage(usage);
            mMetricsRepository.updateNetworkSessionSegment(mCurrentActiveSegment);
        }
    }

    private final Runnable mFlushSessionSegment = new Runnable() {
        @Override
        public void run() {
            //Check if connection exists
            if (mCurrentSession != null) {
                Log.d(TAG, "MMA: Flushing the networks' session information so far. (transport: " + mTransportType + ")");

                long currentTimestamp = System.currentTimeMillis();

                processSessionSegment(mLastReportedSegmentTimestamp, currentTimestamp);
                //Reset counter for next segment
                mTotalRxBytes = 0;
                mTotalTxBytes = 0;
                mLastReportedSegmentTimestamp = currentTimestamp;

                //Start new segment
                createSessionSegment(mLastReportedSegmentTimestamp);
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

    public void setNetworkConnectionEntityAdapter(NetworkConnectionEntityAdapter adapter) {
        mNetworkConnectionEntityAdapter = adapter;
    }

    public interface NetworkConnectionEntityAdapter {
        NetworkConnectionsEntity getEntity();
    }
}