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

import java.util.Queue;
import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import io.grpc.ManagedChannel;
import io.openschema.mma.helpers.ChannelHelper;
import io.openschema.mma.metricsd.MetricsContainer;
import io.openschema.mma.metricsd.MetricsControllerGrpc;
import io.openschema.mma.networking.CertificateManager;

//TODO: add javadocs
public class MetricsWorker extends Worker {

    private static final String TAG = "MetricsWorker";

    public static final String UNIQUE_PERIODIC_WORKER_NAME = "METRICS_PERIODIC";
    private static final String WORKER_TAG = "METRICS_TAG";

    private static final String DATA_CONTROLLER_ADDRESS = "CONTROLLER_ADDRESS";
    private static final String DATA_CONTROLLER_PORT = "CONTROLLER_PORT";
    private static final String DATA_AUTHORITY_HEADER = "AUTHORITY_HEADER";

    private Queue<MetricsContainer> mMetricsQueue;
    private ManagedChannel mChannel;
    private MetricsControllerGrpc.MetricsControllerStub mAsyncStub;
    private MetricsControllerGrpc.MetricsControllerBlockingStub mBlockingStub;

    public MetricsWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);

        Log.d(TAG, "MMA: Initializing MetricsWorker");

        mMetricsQueue = MetricsRepository
                .getRepository(context.getApplicationContext())
                .getQueue();

        //Certificates must have been previously loaded into the KeyStore
        CertificateManager certificateManager = new CertificateManager();

        //Create channel using controller parameters
        Data data = workerParams.getInputData();
        mChannel = ChannelHelper.getSecureManagedChannelWithAuthorityHeader(
                data.getString(DATA_CONTROLLER_ADDRESS),
                data.getInt(DATA_CONTROLLER_PORT, -1),
                certificateManager.generateSSLContext().getSocketFactory(),
                data.getString(DATA_AUTHORITY_HEADER));

        mAsyncStub = MetricsControllerGrpc.newStub(mChannel);
        mBlockingStub = MetricsControllerGrpc.newBlockingStub(mChannel);
    }

    @SuppressLint("CheckResult")
    private void pushMetric(MetricsContainer metricsContainer) {
        //TODO: Use Async method instead and wait for all to complete at the end
        mBlockingStub.collect(metricsContainer);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "MMA: Iterating through " + mMetricsQueue.size() + " metrics...");

        //Iterate through every available metric in the queue
        while (!mMetricsQueue.isEmpty()) {
            MetricsContainer currentMetric = mMetricsQueue.poll();
            pushMetric(currentMetric);
        }

        Log.d(TAG, "MMA: Finished pushing all metrics");

        //Close channel
        mChannel.shutdown();
        return Result.success();
    }

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
