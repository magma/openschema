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

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import io.openschema.mma.helpers.CalendarUtils;

//TODO: javadocs
public class UsageDataWorker extends Worker {
    private static final String TAG = "UsageDataWorker";

    public static final String UNIQUE_PERIODIC_WORKER_NAME = "USAGE_DATA";
    private static final String WORKER_TAG = "USAGE_DATA_TAG";

    private final MetricsManager mMetricsManager;
    private final UsageDataMetrics mUsageDataMetrics;

    public UsageDataWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);

        mMetricsManager = new MetricsManager(context.getApplicationContext());
        mUsageDataMetrics = new UsageDataMetrics(context.getApplicationContext());
    }

    private void collectData() {

        //TODO: check if data has been pushed for this hour
        //TODO: save last pushed hour to sharedprefs
        //TODO: collect data from PREVIOUS hour since the current one hasn't ended yet
        Calendar currentHourCalendar = CalendarUtils.getCurrentHourCalendar();

        mMetricsManager.collect(UsageDataMetrics.METRIC_FAMILY_NAME, mUsageDataMetrics.retrieveHourUsage(currentHourCalendar));
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "MMA: Collecting usage data");

        collectData();

        return Result.success();
    }

    public static void enqueuePeriodicWorker(Context context) {
        PeriodicWorkRequest.Builder workBuilder = new PeriodicWorkRequest.Builder(UsageDataWorker.class, 1, TimeUnit.HOURS)
                .addTag(WORKER_TAG);

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(UNIQUE_PERIODIC_WORKER_NAME, ExistingPeriodicWorkPolicy.REPLACE, workBuilder.build());
    }
}
