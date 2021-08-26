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
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import androidx.annotation.NonNull;
import androidx.core.util.Pair;
import io.openschema.mma.data.MetricsRepository;
import io.openschema.mma.data.entity.NetworkQualityEntity;
import io.openschema.mma.utils.DnsTester;
import io.openschema.mma.utils.SignalStrength;

/**
 * Class to collect information about Network Quality, be it QoS or QoE.
 */
public class NetworkQualityMetrics extends AsyncMetrics {
    private static final String TAG = "NetworkQualityMetrics";

    /**
     * Metric name to be used for the collected information.
     */
    public static final String METRIC_NAME = "openschemaNetworkQuality";

    public static final String METRIC_QUALITY_SCORE = "quality_score";
    public static final String METRIC_LATENCY = "latency";
    public static final String METRIC_RSSI = "rssi";

    private final MetricsCollectorListener mListener;
    private final MetricsRepository mMetricsRepository;
    private final ConnectivityManager mConnectivityManager;
    private final SignalStrength mSignalStrength;
    private final ExecutorService mExecutorService;
    private Future<?> mLastRequestFuture = null;

    private final ConnectivityManager.NetworkCallback mNetworkCallBack;
    private ConnectivityManager.NetworkCallback getNetworkCallback() {
        return new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                Log.d(TAG, "MMA: Detected new active network connection");

                //Check if the previous test is still running and cancel it. Due to active network changes the results won't be relevant anymore.
                //TODO: Move to onLost instead?
                if (mLastRequestFuture != null && !mLastRequestFuture.isDone()) {
                    mLastRequestFuture.cancel(true);
                }

                NetworkCapabilities networkCapabilities = mConnectivityManager.getNetworkCapabilities(network);
                if (networkCapabilities != null) {
                    int transportType = -1;
                    if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                        Log.d(TAG, "MMA: Active network is now WIFI");
                        transportType = NetworkCapabilities.TRANSPORT_WIFI;
                    } else if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                        Log.d(TAG, "MMA: Active network is now CELLULAR");
                        transportType = NetworkCapabilities.TRANSPORT_CELLULAR;
                    }

                    if (transportType == -1) {
                        Log.e(TAG, "MMA: Couldn't recognize active network type");
                        return;
                    }

                    //Get the connection ID used in the local DB for the active network
                    int activeConnectionId = mActiveConnectionRetriever.getActiveConnectionId(transportType);
                    if (activeConnectionId == -1) {
                        Log.e(TAG, "MMA: Active connection ID failed to be retrieved");
                    }
                    requestMetrics(activeConnectionId, transportType);
                }
            }
        };
    }

    private final ActiveConnectionRetriever mActiveConnectionRetriever;

    public NetworkQualityMetrics(Context context, MetricsCollectorListener listener, ActiveConnectionRetriever activeConnectionRetriever) {
        super(context);
        mListener = listener;
        mSignalStrength = new SignalStrength(context);
        mExecutorService = Executors.newCachedThreadPool();
        mConnectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        mNetworkCallBack = getNetworkCallback();
        mMetricsRepository = MetricsRepository.getRepository(context.getApplicationContext());
        mActiveConnectionRetriever = activeConnectionRetriever;
    }

    private void requestMetrics(final int networkConnectionId, final int transportType) {

        mLastRequestFuture = mExecutorService.submit(() -> {
            List<Pair<String, String>> networkQualityMetrics = runTests(networkConnectionId, transportType);
            //Collect metrics to DB to be pushed later
            mListener.onMetricCollected(METRIC_NAME, networkQualityMetrics);
        });
    }

    private List<Pair<String, String>> runTests(final int networkConnectionId, final int transportType) {
        Log.d(TAG, "MMA: Generating network quality metrics...");
        List<Pair<String, String>> metricsList = new ArrayList<>();

        //Latency
        //TODO: implement proper test using device's default server and others
        double rtt = DnsTester.testServer("1.1.1.1");

        //RSSI
        int rssi = mSignalStrength.getRSSI(transportType);

        //Final QoS/QoE score
        int score = calculateQualityScore(rtt, rssi);

        //Extract information shared by both network types
        metricsList.add(new Pair<>(METRIC_QUALITY_SCORE, Integer.toString(score)));
        metricsList.add(new Pair<>(METRIC_LATENCY, Double.toString(rtt)));
        metricsList.add(new Pair<>(METRIC_RSSI, Integer.toString(rssi)));

        //Write to local DB
        writeNetworkQuality(new NetworkQualityEntity(networkConnectionId, transportType, score, rtt, rssi, System.currentTimeMillis()));

        Log.d(TAG, "MMA: Collected report:\n" + metricsList.toString());
        return metricsList;
    }

    private int calculateQualityScore(double rtt, int rssi) {
        //TODO: Implement equation
        return 0;
    }

    private void writeNetworkQuality(NetworkQualityEntity networkQualityEntity) {
        //TODO: disable with flag from MMA builder? avoid extra calculations & storage
        Log.d(TAG, "MMA: Creating new database entry for network quality.");
        //TODO: Evaluate possible concurrency issues by using the repo object from background threads
        mMetricsRepository.writeNetworkQuality(networkQualityEntity);
    }

    public void startTrackers() {
        NetworkRequest activeNetworkRequest = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build();

        mConnectivityManager.requestNetwork(activeNetworkRequest, mNetworkCallBack);
    }

    //Stops tracking the network's changes
    public void stopTrackers() {
        mConnectivityManager.unregisterNetworkCallback(mNetworkCallBack);
    }

    //Interface to supply the metrics class with the network connection's DB entry ID
    public interface ActiveConnectionRetriever {
        int getActiveConnectionId(int transportType);
    }
}
