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

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import io.openschema.mma.data.MetricsEntity;
import io.openschema.mma.id.Identity;
import io.openschema.mma.networking.BackendApi;
import io.openschema.mma.networking.RetrofitService;
import io.openschema.mma.networking.request.MetricsPushRequest;
import io.openschema.mma.networking.response.BaseResponse;
import retrofit2.Response;

/**
 * Background worker that flushes the metrics queue and sends all
 * saved metrics to the controller.
 */
public class MetricsWorker extends Worker {

    private static final String TAG = "MetricsWorker";

    public static final String UNIQUE_PERIODIC_WORKER_NAME = "METRICS_PERIODIC";
    private static final String WORKER_TAG = "METRICS_TAG";

    private final MetricsRepository mMetricsRepository;

    private final List<MetricsEntity> mMetricsList;
    private Identity mIdentity;

    private final BackendApi mBackendApi;

    public MetricsWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);

        Log.d(TAG, "MMA: Initializing MetricsWorker");

        mMetricsRepository = MetricsRepository.getRepository(context.getApplicationContext());

        //Load queue from repository
        mMetricsList = mMetricsRepository.getEnqueuedMetrics();

        //Identity must have been previously generated during initialization
        try {
            mIdentity = new Identity(context);
        } catch (Exception e) {
            e.printStackTrace();
        }

        //TODO: might need to re-initialize the API in case the memory gets wiped
        mBackendApi = RetrofitService.getService(context.getApplicationContext()).getApi();
    }

    @NonNull
    @Override
    public Result doWork() {

        Log.d(TAG, "MMA: Starting background job to push queued metrics");

        if (mBackendApi == null) {
            Log.e(TAG, "MMA: Retrofit API for OpenSchema ETL hasn't been initialized");
            return Result.failure();
        }

        //TODO: Optimize to either:
        //      A) Batch several metrics in a single POST,
        //      B) Execute multiple POST requests asynchronously rather than in sequence
        //TODO: reduce debug logging
        try {
            for (MetricsEntity currentMetric : mMetricsList) {
                Log.d(TAG, "MMA: Sending push request...");
                Response<BaseResponse> res = mBackendApi.pushMetric(new MetricsPushRequest(currentMetric.mMetricName, currentMetric.mMetricsList, mIdentity.getUUID(), currentMetric.mTimeStamp))
                        .execute();

                if (res.isSuccessful()) {
                    Log.d(TAG, "MMA: onResponse success: " + res.body().getMessage());
                    Log.d(TAG, "MMA: Metric was pushed successfully");
                } else {
                    String errorMessage = BaseResponse.getErrorMessage(res.errorBody());
                    Log.d(TAG, "MMA: onResponse failure (" + res.code() + "): " + errorMessage);
                }
            }
        } catch (IOException e) {
            Log.d(TAG, "MMA: Failure communicating with OpenSchema ETL");
            e.printStackTrace();
            return Result.retry();
        }

        Log.d(TAG, "MMA: Finished pushing all metrics");

        //Clear the pushed metrics from the database
        mMetricsRepository.clearMetrics(mMetricsList);

        return Result.success();
    }

    /**
     * Static utility method to enqueue this worker to run periodically. Calling this method
     * will cause the worker to run immediately and restart the periodic calls delay counter.
     */
    public static void enqueuePeriodicWorker(Context context) {
        PeriodicWorkRequest.Builder workBuilder = new PeriodicWorkRequest.Builder(MetricsWorker.class, 4, TimeUnit.HOURS)
                .addTag(WORKER_TAG);

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(UNIQUE_PERIODIC_WORKER_NAME, ExistingPeriodicWorkPolicy.REPLACE, workBuilder.build());
    }
}
