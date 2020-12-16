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
import android.content.Context;
import android.util.Log;

import java.util.List;
import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import io.grpc.ManagedChannel;
import io.openschema.mma.data.MetricsEntity;
import io.openschema.mma.helpers.ChannelHelper;
import io.openschema.mma.id.Identity;
import io.openschema.mma.metricsd.MetricsContainer;
import io.openschema.mma.metricsd.MetricsControllerGrpc;
import io.openschema.mma.networking.CertificateManager;

/**
 * Background worker that flushes the metrics queue and sends all
 * saved metrics to the controller.
 */
public class MetricsWorker extends Worker {

    private static final String TAG = "MetricsWorker";

    public static final String UNIQUE_PERIODIC_WORKER_NAME = "METRICS_PERIODIC";
    private static final String WORKER_TAG = "METRICS_TAG";

    private static final String DATA_CONTROLLER_ADDRESS = "CONTROLLER_ADDRESS";
    private static final String DATA_CONTROLLER_PORT = "CONTROLLER_PORT";
    private static final String DATA_AUTHORITY_HEADER = "AUTHORITY_HEADER";

    private final MetricsRepository mMetricsRepository;

    private final List<MetricsEntity> mMetricsList;
    private Identity mIdentity;
    private final ManagedChannel mChannel;
    private final MetricsControllerGrpc.MetricsControllerBlockingStub mBlockingStub;

    public MetricsWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);

        Log.d(TAG, "MMA: Initializing MetricsWorker");

        mMetricsRepository = MetricsRepository.getRepository(context.getApplicationContext());

        //Load queue from repository
        mMetricsList = mMetricsRepository.getEnqueuedMetrics();

        //TODO: Consider adding a SharedPreference to check if bootstrapping has been completed yet,
        // otherwise retry later.
        //Identity must have been previously generated during initialization
        try {
            mIdentity = new Identity(context);
        } catch (Exception e) {
            e.printStackTrace();
        }

        //Certificates must have been previously loaded into the KeyStore
        CertificateManager certificateManager = new CertificateManager();

        //Create channel using controller parameters
        Data data = workerParams.getInputData();
        mChannel = ChannelHelper.getSecureManagedChannelWithAuthorityHeader(
                data.getString(DATA_CONTROLLER_ADDRESS),
                data.getInt(DATA_CONTROLLER_PORT, -1),
                certificateManager.generateSSLContext().getSocketFactory(),
                data.getString(DATA_AUTHORITY_HEADER));

        mBlockingStub = MetricsControllerGrpc.newBlockingStub(mChannel);
    }

    /**
     * Uses the GRPC stub to connect to the controller and send the data.
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    @SuppressLint("CheckResult")
    private void pushMetric(MetricsContainer metricsContainer) {
        mBlockingStub.collect(metricsContainer);
    }

    /**
     * Iterate through all the metrics saved to the database & batch them
     * together in a single container for pushing to the cloud.
     */
    private MetricsContainer buildMetricsContainer() {
        MetricsContainer.Builder metricsContainerBuilder = MetricsContainer.newBuilder()
                .setGatewayId(mIdentity.getUUID());

        //Iterate through every metric family in the database
        Log.d(TAG, "MMA: Iterating through " + mMetricsList.size() + " metrics...");
        for (MetricsEntity currentMetric : mMetricsList) {
            Metric.Builder metricBuilder = Metric.newBuilder();

            //Iterate through every metric pair in the family
            for (int i = 0; i < currentMetric.mMetrics.size(); i++) {
                metricBuilder.addLabel(LabelPair.newBuilder()
                        .setName(currentMetric.mMetrics.get(i).first)
                        .setValue(currentMetric.mMetrics.get(i).second)
                        .build());
            }

            //Initialize fields used by Magma
            metricBuilder
                    .addLabel(LabelPair.newBuilder()
                            .setName(MetricsManager.METRIC_UUID)
                            .setValue(mIdentity.getUUID())
                            .build())
                    .addLabel(LabelPair.newBuilder()
                            .setName(MetricsManager.METRIC_TIMESTAMP)
                            .setValue(currentMetric.mTimeStamp)
                            .build())
                    .setUntyped(Untyped.newBuilder()
                            .setValue(0)
                            .build());

            //Build the metric family and add it to the container
            metricsContainerBuilder.addFamily(MetricFamily.newBuilder()
                    .setName(currentMetric.mFamilyName)
                    .setType(MetricType.UNTYPED)
                    .addMetric(metricBuilder.build())
                    .build()
            );
        }

        return metricsContainerBuilder.build();
    }

    @NonNull
    @Override
    public Result doWork() {

        Log.d(TAG, "MMA: Starting background job to push queued metrics");

        //TODO: Catch an ExecutionException or IOException and retry later.
        // If the metric worker starts before bootstrapping has been completed
        // it might throw an exception.
        //Send all the metrics batched into a single container
        pushMetric(buildMetricsContainer());

        Log.d(TAG, "MMA: Finished pushing all metrics");

        //Close GRPC channel
        mChannel.shutdown();

        //Clear the pushed metrics from the database
        mMetricsRepository.clearMetrics(mMetricsList);

        return Result.success();
    }

    /**
     * Static utility method to enqueue this worker to run periodically. Calling this method
     * will cause the worker to run immediately and restart the periodic calls delay counter.
     *
     * @param metricsControllerAddress Address of the magma controller.
     * @param metricsControllerPort    Port used by the magma controller.
     * @param metricsAuthorityHeader   Header used to override the TLS/HTTP authority.
     */
    public static void enqueuePeriodicWorker(Context context, String metricsControllerAddress, int metricsControllerPort, String metricsAuthorityHeader) {
        PeriodicWorkRequest.Builder workBuilder = new PeriodicWorkRequest.Builder(MetricsWorker.class, 4, TimeUnit.HOURS)
                .addTag(WORKER_TAG)
                .setInputData(new Data.Builder()
                        .putString(DATA_CONTROLLER_ADDRESS, metricsControllerAddress)
                        .putInt(DATA_CONTROLLER_PORT, metricsControllerPort)
                        .putString(DATA_AUTHORITY_HEADER, metricsAuthorityHeader)
                        .build());

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(UNIQUE_PERIODIC_WORKER_NAME, ExistingPeriodicWorkPolicy.REPLACE, workBuilder.build());
    }
}
