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

import android.content.Context;
import android.util.Log;

import java.util.List;

import androidx.core.util.Pair;
import io.openschema.mma.id.Identity;
import io.openschema.mma.metricsd.MetricsContainer;

/**
 * Class in charge of handling pushing metrics to the controller.
 */
public class MetricsManager {

    private static final String TAG = "MetricsManager";

    private static final String METRIC_UUID = "uuid";
    private static final String METRIC_TIMESTAMP = "timestamp";

    private MetricsRepository mMetricsRepository;
    private Identity mIdentity;

    public MetricsManager(Context appContext, String metricsControllerAddress, int metricsControllerPort, String metricsAuthorityHeader, Identity identity) {
        mMetricsRepository = MetricsRepository.getRepository(appContext);
        mIdentity = identity;

        MetricsWorker.enqueuePeriodicWorker(appContext, metricsControllerAddress, metricsControllerPort, metricsAuthorityHeader);
    }

    /**
     * Send metrics to prometheus through GRPC using the Collect method in metricsd.proto
     *
     * @param metricName   Root name for the group of collected metrics
     * @param metricValues List of metrics to collect with the <name, value> structure
     */
    public void collect(String metricName, List<Pair<String, String>> metricValues) {
        Log.d(TAG, "MMA: Collecting metric \"" + metricName + "\"");

        //Create metrics base
        Metric.Builder metricBuilder = Metric.newBuilder();

        //Add custom list of metrics
        for (Pair<String, String> metricValue : metricValues) {
            metricBuilder.addLabel(LabelPair.newBuilder()
                    .setName(metricValue.first)
                    .setValue(metricValue.second)
                    .build());
        }

        //Send metric to be handled
        collect(buildMMAContainer(metricName, metricBuilder));
    }

    /**
     * Generates a {@link MetricsContainer} object and adds information
     * required by the Magma controller.
     */
    private MetricsContainer buildMMAContainer(String metricName, Metric.Builder metricBuilder) {
        long timestamp = System.currentTimeMillis();

        metricBuilder
                .addLabel(LabelPair.newBuilder()
                        .setName(METRIC_UUID)
                        .setValue(mIdentity.getUUID())
                        .build())
                .addLabel(LabelPair.newBuilder()
                        .setName(METRIC_TIMESTAMP)
                        .setValue(Long.toString(timestamp))
                        .build())
                .setUntyped(Untyped.newBuilder()
                        .setValue(0)
                        .build());

        //Create magma wrapper
        return MetricsContainer.newBuilder()
                .setGatewayId(mIdentity.getUUID())
                .addFamily(MetricFamily.newBuilder()
                        .setName(metricName)
                        .setType(MetricType.UNTYPED)
                        .addMetric(metricBuilder.build())
                        .build())
                .build();
    }

    /**
     * Sends the metrics object to the repository to be stored in a queue for batching.
     */
    private void collect(MetricsContainer metricsContainer) {
        mMetricsRepository.queueMetric(metricsContainer);
    }

//    /**
//     * Send metrics to prometheus through GRPC using the Push method in metricsd.proto
//     *
//     * @param metricName   Root name for the group of collected metrics
//     * @param metricValues List of metrics to collect with the <name, value> structure
//     */
//    @SuppressLint("CheckResult")
//    @WorkerThread
//    public void pushSync(String metricName, List<Pair<String, String>> metricValues) {
//
//        Log.d(TAG, "MMA: Sending push request...");
//
//        PushedMetric.Builder metricBuilder = PushedMetric.newBuilder()
//                .setMetricName(metricName)
//                .setTimestampMS(System.currentTimeMillis());
//
//        for (Pair<String, String> metricValue : metricValues) {
//            metricBuilder.addLabels(LabelPair.newBuilder()
//                    .setName(metricValue.first)
//                    .setValue(metricValue.second)
//                    .build());
//        }
//
//        PushedMetricsContainer payload = PushedMetricsContainer.newBuilder()
//                .setNetworkId(mIdentity.getUUID())
//                .addMetrics(metricBuilder.build())
//                .build();
//
//        mBlockingStub.push(payload);
//    }
}
