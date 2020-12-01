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

import android.annotation.SuppressLint;
import android.util.Log;

import java.util.List;

import javax.net.ssl.SSLContext;

import androidx.annotation.WorkerThread;
import androidx.core.util.Pair;
import io.grpc.Channel;
import io.openschema.mma.helpers.ChannelHelper;
import io.openschema.mma.id.Identity;
import io.openschema.mma.metricsd.MetricsContainer;
import io.openschema.mma.metricsd.MetricsControllerGrpc;
import io.openschema.mma.metricsd.PushedMetric;
import io.openschema.mma.metricsd.PushedMetricsContainer;

/**
 * Class in charge of handling the metrics calls.
 */
public class MetricsManager {

    private static final String TAG = "MetricsManager";

    private Identity mIdentity;
    private MetricsControllerGrpc.MetricsControllerBlockingStub mBlockingStub;

    public MetricsManager(String metricsControllerAddress, int metricsControllerPort, String metricsAuthorityHeader, SSLContext sslContext, Identity identity) {
        mIdentity = identity;

        Channel channel = ChannelHelper.getSecureManagedChannelwithAuthorityHeader(
                metricsControllerAddress,
                metricsControllerPort,
                sslContext.getSocketFactory(),
                metricsAuthorityHeader);

        mBlockingStub = MetricsControllerGrpc.newBlockingStub(channel);
    }

    /**
     * Send metrics to prometheus through GRPC using the Collect method in metricsd.proto
     *
     * @param metricName   Root name for the group of collected metrics
     * @param metricValues List of metrics to collect with the <name, value> structure
     */
    @SuppressLint("CheckResult")
    @WorkerThread
    public void collectSync(String metricName, List<Pair<String, String>> metricValues) {

        Log.d(TAG, "MMA: Sending collect request...");

        long timestamp = System.currentTimeMillis();

        //Create metrics base
        Metric.Builder metricBuilder = Metric.newBuilder()
//                .setTimestampMs(timestamp) //Not sure where this is used
                .addLabel(LabelPair.newBuilder()
                        .setName("uuid")
                        .setValue(mIdentity.getUUID())
                        .build())
                .addLabel(LabelPair.newBuilder()
                        .setName("timestamp")
                        .setValue(Long.toString(timestamp))
                        .build())
                .setUntyped(Untyped.newBuilder()
                        .setValue(0)
                        .build());

        //Add custom list of metrics
        for (Pair<String, String> metricValue : metricValues) {
            metricBuilder.addLabel(LabelPair.newBuilder()
                    .setName(metricValue.first)
                    .setValue(metricValue.second)
                    .build());
        }

        //Create magma wrapper
        MetricsContainer metricsContainer = MetricsContainer.newBuilder()
                .setGatewayId(mIdentity.getUUID())
                .addFamily(MetricFamily.newBuilder()
                        .setName(metricName)
                        .setType(MetricType.UNTYPED)
                        .addMetric(metricBuilder.build())
                        .build())
                .build();

        //Send metric through grpc
        mBlockingStub.collect(metricsContainer);
    }

    /**
     * Send metrics to prometheus through GRPC using the Push method in metricsd.proto
     *
     * @param metricName   Root name for the group of collected metrics
     * @param metricValues List of metrics to collect with the <name, value> structure
     */
    @SuppressLint("CheckResult")
    @WorkerThread
    public void pushSync(String metricName, List<Pair<String, String>> metricValues) {

        Log.d(TAG, "MMA: Sending push request...");

        PushedMetric.Builder metricBuilder = PushedMetric.newBuilder()
                .setMetricName(metricName)
                .setTimestampMS(System.currentTimeMillis());

        for (Pair<String, String> metricValue : metricValues) {
            metricBuilder.addLabels(LabelPair.newBuilder()
                    .setName(metricValue.first)
                    .setValue(metricValue.second)
                    .build());
        }

        PushedMetricsContainer payload = PushedMetricsContainer.newBuilder()
                .setNetworkId(mIdentity.getUUID())
                .addMetrics(metricBuilder.build())
                .build();

        mBlockingStub.push(payload);
    }
}
