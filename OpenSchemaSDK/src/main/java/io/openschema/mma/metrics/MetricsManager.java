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

import android.util.Log;

import javax.net.ssl.SSLContext;

import androidx.annotation.WorkerThread;
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

    @WorkerThread
    public void collectSync(String metricName, String metricValue) {

        Log.d(TAG, "MMA: Sending collect request...");
        LabelPair labelPair = LabelPair.newBuilder()
                .setName(metricName)
                .setValue(metricValue)
                .build();

        Metric metric = Metric.newBuilder()
                .addLabel(labelPair)
                .setTimestampMs(System.currentTimeMillis())
                .build();

        MetricFamily metricFamily = MetricFamily.newBuilder()
                .addMetric(metric)
                .build();

        MetricsContainer payload = MetricsContainer.newBuilder()
                .setGatewayId(mIdentity.getUUID())
                .addFamily(metricFamily)
                .build();


//        mAsyncStub.collect(payload, new StreamObserver<Void>() {
//            @Override
//            public void onNext(Void value) {
//                Log.d(TAG, "MMA: COLLECT onNext: ");
//            }
//            @Override
//            public void onError(Throwable t) {
//                Log.d(TAG, "MMA: COLLECT onError: ");
//                t.printStackTrace();
//            }
//            @Override
//            public void onCompleted() {
//                Log.d(TAG, "MMA: COLLECT onCompleted: ");
//            }
//        });
//        mBlockingStub.collect(payload);

        //TODO: Remove test metric and allow for custom ones to be sent
        MetricsContainer mMetricContainer = MetricsContainer.newBuilder()
                .setGatewayId(mIdentity.getUUID())
                .addFamily(MetricFamily.newBuilder()
                        .setName("location")
                        .addMetric(Metric.newBuilder()
                                .addLabel(LabelPair.newBuilder()
                                        .setName("lat")
                                        .setValue("0.1")
                                        .build())
                                .addLabel(LabelPair.newBuilder()
                                        .setName("long")
                                        .setValue("0.2")
                                        .build())
                                .addLabel(LabelPair.newBuilder()
                                        .setName("timestamp")
                                        .setValue(Long.toString(System.currentTimeMillis()))
                                        .build())
                                .addLabel(LabelPair.newBuilder()
                                        .setName("uuid")
                                        .setValue(mIdentity.getUUID())
                                        .build())
                                .setUntyped(Untyped.newBuilder()
                                        .setValue(0)
                                        .build())
                                .build())
                        .setType(MetricType.UNTYPED)
                        .build())
                .build();

        mBlockingStub.collect(mMetricContainer);
    }

    @WorkerThread
    public void pushSync(String metricName, String metricValue) {

        Log.d(TAG, "MMA: Sending push request...");
        LabelPair labelPair = LabelPair.newBuilder()
                .setName(metricName)
                .setValue(metricValue)
                .build();

        PushedMetric metric = PushedMetric.newBuilder()
                .setMetricName(metricName)
                .addLabels(labelPair)
                .setTimestampMS(System.currentTimeMillis())
                .build();

        PushedMetricsContainer payload = PushedMetricsContainer.newBuilder()
                .setNetworkId(mIdentity.getUUID())
                .addMetrics(metric)
                .build();

//        mAsyncStub.push(payload, new StreamObserver<Void>() {
//            @Override
//            public void onNext(Void value) {
//                Log.d(TAG, "MMA: PUSH onNext: ");
//            }
//            @Override
//            public void onError(Throwable t) {
//                Log.d(TAG, "MMA: PUSH onError: ");
//                t.printStackTrace();
//            }
//            @Override
//            public void onCompleted() {
//                Log.d(TAG, "MMA: PUSH onCompleted: ");
//            }
//        });
        mBlockingStub.push(payload);
    }
}
