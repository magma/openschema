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
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import androidx.annotation.NonNull;
import androidx.core.util.Pair;
import io.openschema.mma.data.MetricsRepository;
import io.openschema.mma.data.entity.NetworkQualityEntity;
import io.openschema.mma.utils.DnsServersDetector;
import io.openschema.mma.utils.DnsTester;
import io.openschema.mma.utils.QosInfo;
import io.openschema.mma.utils.SignalStrength;
import io.openschema.mma.utils.TransportType;

/**
 * Class to collect information about Network Quality, be it QoS or QoE.
 */
public class NetworkQualityMetrics extends AsyncMetrics {
    private static final String TAG = "NetworkQualityMetrics";

    private final Context mContext;
    /**
     * Metric name to be used for the collected information.
     */
    public static final String METRIC_NAME = "openschemaNetworkQuality";

    public static final String METRIC_QUALITY_SCORE = "qualityScore";
    public static final String METRIC_LATENCY = "latency";
    public static final String METRIC_RSSI = "rssi";

    private final MetricsCollectorListener mListener;
    private final MetricsRepository mMetricsRepository;
    private final ConnectivityManager mConnectivityManager;
    private final SignalStrength mSignalStrength;
    private final ExecutorService mExecutorService;

    private Future<?> mLastRequestFuture = null;
    private int mCurrentActiveTransportType = -1;
    private int mCurrentActiveConnectionId = -1;

    private final ConnectivityManager.NetworkCallback mNetworkCallBack;
    private ConnectivityManager.NetworkCallback getNetworkCallback() {
        return new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                Log.d(TAG, "MMA: Detected new active network connection");

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

                    mCurrentActiveTransportType = transportType;

                    //TODO: Figure out how to handle the case where the connection entry hasn't finished writing to DB yet.
                    //Get the connection ID used in the local DB for the active network
                    mCurrentActiveConnectionId = mActiveConnectionRetriever.getActiveConnectionId(mCurrentActiveTransportType);
                    if (mCurrentActiveConnectionId == -1) {
                        Log.e(TAG, "MMA: Active connection ID failed to be retrieved");
                    }
                    requestMetrics(mCurrentActiveConnectionId, mCurrentActiveTransportType);
                }
            }

            @Override
            public void onLost(@NonNull Network network) {
                mCurrentActiveTransportType = -1;
                mCurrentActiveConnectionId = -1;

                //Check if the previous test is still running and cancel it. Due to active network changes the results won't be relevant anymore.
                if (mLastRequestFuture != null && !mLastRequestFuture.isDone()) {
                    mLastRequestFuture.cancel(true);
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
        mContext = context;
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

        //Latency / RTT
        Pair<List<QosInfo>, List<QosInfo>> rttTestsResults = runRttTests();
        //Using average of default DNS servers as a placeholder.
        double rtt = rttTestsResults.first.stream()
                .mapToDouble(QosInfo::getRttMean)
                .average()
                .orElse(Double.NaN);

        //RSSI
        int rssi = mSignalStrength.getRSSI(transportType);

        //Final QoS/QoE score
        double score = calculateQualityScore(rttTestsResults);

        //Extract information shared by both network types
        List<Pair<String, String>> metricsList = new ArrayList<>();
        metricsList.add(new Pair<>(TransportType.METRIC_TRANSPORT_TYPE, TransportType.getTransportString(transportType)));
        metricsList.add(new Pair<>(METRIC_QUALITY_SCORE, Double.toString(score)));
        metricsList.add(new Pair<>(METRIC_LATENCY, Double.toString(rtt)));
        metricsList.add(new Pair<>(METRIC_RSSI, Integer.toString(rssi)));

        //Write to local DB
        writeNetworkQuality(new NetworkQualityEntity(networkConnectionId, transportType, score, rtt, rssi, System.currentTimeMillis()));

        Log.d(TAG, "MMA: Collected report:\n" + metricsList.toString());
        return metricsList;
    }

    private Pair<List<QosInfo>, List<QosInfo>> runRttTests() {
        DnsTester.randomizeDomains();
        List<QosInfo> testDnsServers = DnsTester.testDefaultServers();
        DnsServersDetector mDnsServersDetector = new DnsServersDetector(mContext);
        DnsTester.randomizeDomains();
        List<QosInfo> deviceDnsServers = DnsTester.testServers(mDnsServersDetector.getServers());
        return new Pair<>(testDnsServers, deviceDnsServers);
    }

    private double calculateQualityScore(Pair<List<QosInfo>, List<QosInfo>> rttTestsResults) {

        //For the default DNS we will be using the one that returns the lowest RTTs for now from the ones returned from mDnsServersDetector.getServers()
        //Step 1: Get the min RTT from default DNS. And make sure test is usable:
        //1- each dns test should have at least 50% success to be considered for the calculation.
        //2- Default DNS tes should be success
        //3- at least 50% of non default DNS test should be success

        QosInfo minDefaultRttServer = rttTestsResults.second.get(0);

        for (int i = 1; i < rttTestsResults.second.size(); i++) {
            //TODO: Handle 0(failed RTT's) in a better way
            if(rttTestsResults.second.get(i).getSuccessRate() < 0.5) {
                continue;
            }

            if (rttTestsResults.second.get(i).getMinRTTValue() == 0) {
                continue;
            }
            if(minDefaultRttServer.getMinRTTValue() == 0){
                minDefaultRttServer = rttTestsResults.second.get(i);
            }
            if (rttTestsResults.second.get(i).getMinRTTValue() < minDefaultRttServer.getMinRTTValue()) {
                minDefaultRttServer = rttTestsResults.second.get(i);
            }
        }

        //TODO: Handle no default DNS, Maybe throw an error.
        //Conditions Default DNS
        //Default DNS has to have a successRate of 0.5 or greater.
        if(minDefaultRttServer.getRttMean() == 0 || minDefaultRttServer.getSuccessRate() < 0.5){
            Log.d(TAG, "No default DNS matches the criteria to calculate QoS\n");
            return -1;
        }

        double totalSuccessfulHardcodedDNS = 0.0;
        for (int i = 0; i < rttTestsResults.first.size(); i++) {
            if(rttTestsResults.first.get(i).getSuccessRate() >= 0.5) {
                totalSuccessfulHardcodedDNS = totalSuccessfulHardcodedDNS + 1.0;
            }
        }
        Log.d(TAG, "Hardcoded DNSs Total successful Tests: " + totalSuccessfulHardcodedDNS + " and Total hardcoded DNSs: " + rttTestsResults.first.size());

        double totalSuccessfulHardcodedDNSPercentage = totalSuccessfulHardcodedDNS/rttTestsResults.first.size();
        if(totalSuccessfulHardcodedDNSPercentage < 0.5) {
            Log.d(TAG, "Hardcoded Dns Servers success rate:\n" + Double.toString(totalSuccessfulHardcodedDNSPercentage));
            return -1;
        }

        Log.d(TAG, "Default DNS Success Rate:\n" + minDefaultRttServer.getSuccessRate());
        Log.d(TAG, "Default Min RTT:\n" + minDefaultRttServer.getMinRTTValue());

        //Step 2: Calculate confidence Factor: (percentage of non default success test) + (average of success's of all tests) / 2
        double confidenceFactor;
        double successfulNonDefaultTests = 0;
        double nonDefaultSuccessRate;
        double averageSuccessRate = 0.0;

        for (int i = 0; i < rttTestsResults.first.size(); i++) {

            averageSuccessRate = averageSuccessRate + rttTestsResults.first.get(i).getSuccessRate();

            if(rttTestsResults.first.get(i).getSuccessRate() >= 0.5) {
                successfulNonDefaultTests = successfulNonDefaultTests++;
            }
        }

        nonDefaultSuccessRate = successfulNonDefaultTests/rttTestsResults.first.size();
        averageSuccessRate = (averageSuccessRate + minDefaultRttServer.getSuccessRate())/(rttTestsResults.first.size()+1);

        confidenceFactor = nonDefaultSuccessRate + (averageSuccessRate/2);
        Log.d(TAG, "QOS Confidence Factor:\n" + confidenceFactor);

        //Step 3: Clean Data
        minDefaultRttServer.cleanData();
        for (int i = 0; i < rttTestsResults.first.size(); i++) {
            rttTestsResults.first.get(i).cleanData();
        }

        //Step 4: Map Mean RTT of the default server to scoring scale, for now we call this scale Pivot Scale.

        int pivotScore = 0;
        ///Not using switch since it doesn't accept long type
        if (minDefaultRttServer.getRttMean() < 50) pivotScore = 5;
        else if (minDefaultRttServer.getRttMean() >= 50 && minDefaultRttServer.getRttMean() < 75) pivotScore = 4;
        else if (minDefaultRttServer.getRttMean() >= 75 && minDefaultRttServer.getRttMean() < 100) pivotScore = 3;
        else if (minDefaultRttServer.getRttMean() >= 100 && minDefaultRttServer.getRttMean() < 125) pivotScore = 2;
        else if (minDefaultRttServer.getRttMean() >= 125) pivotScore = 1;

        Log.d(TAG, "Pivot Score:\n" + pivotScore);

        //Step 5: Scale other result of default DNS

        ArrayList<Long> defaultDNStRTTS = minDefaultRttServer.getRttValues();
        double[] scaledDefaultDNSRTTS = new double[defaultDNStRTTS.size()];

        for(int i = 0; i < defaultDNStRTTS.size(); i++) {
            scaledDefaultDNSRTTS[i] = (pivotScore * minDefaultRttServer.getRttMean())/defaultDNStRTTS.get(i);
            if(scaledDefaultDNSRTTS[i] < 1.0) scaledDefaultDNSRTTS[i] = 1.0;
            if(scaledDefaultDNSRTTS[i] > 5.0) scaledDefaultDNSRTTS[i] = 5.0;

        }

        //Step 6: Calculate mean of all standard deviations
        double averageStdDev = 0.0;
        for (int i = 0; i < rttTestsResults.first.size(); i++) {
            averageStdDev = averageStdDev + rttTestsResults.first.get(i).getRttStdDev();
        }

        averageStdDev = (averageStdDev + minDefaultRttServer.getRttStdDev())/(rttTestsResults.first.size() +1);

        //Step 7: Map step 6 result to pivot range:
        int averageStdDevScore = 0;
        ///Not using switch since it doesn't accept long type
        if (averageStdDev < 50) averageStdDevScore = 5;
        else if (averageStdDev >= 50 && averageStdDev < 75) averageStdDevScore = 4;
        else if (averageStdDev >= 75 && averageStdDev < 100) averageStdDevScore = 3;
        else if (averageStdDev >= 100 && averageStdDev < 125) averageStdDevScore = 2;
        else if (averageStdDev >= 125) averageStdDevScore = 1;

        Log.d(TAG, "Average StdDev Score:\n" + averageStdDevScore);

        //Step 8: Calculate QoS score -> .7(average of step5) + .3 of step 7
        double averageScaledDefaultDNSRTTS = Arrays.stream(scaledDefaultDNSRTTS).average().orElse(0.0);
        if (averageScaledDefaultDNSRTTS > 5.0) averageScaledDefaultDNSRTTS = 5.0;
        if (averageScaledDefaultDNSRTTS < 1.0) averageScaledDefaultDNSRTTS = 1.0;
        double qosScore = 0.7 * averageScaledDefaultDNSRTTS + 0.3 * averageStdDevScore;
        Log.d(TAG, "Final QoS Score:\n" + qosScore);

        return qosScore;
    }

    private void writeNetworkQuality(NetworkQualityEntity networkQualityEntity) {
        //TODO: disable with flag from MMA builder? avoid extra calculations & storage
        Log.d(TAG, "MMA: Creating new database entry for network quality.");
        //TODO: Evaluate possible concurrency issues by using the repo object from background threads
        mMetricsRepository.writeNetworkQuality(networkQualityEntity);
    }

    public void remeasureQuality() {
        if (mCurrentActiveTransportType == -1) {
            Log.e(TAG, "MMA: Couldn't recognize active network type");
            return;
        }

        requestMetrics(mCurrentActiveConnectionId, mCurrentActiveTransportType);
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
