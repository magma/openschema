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
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import io.openschema.mma.data.MMADatabase;
import io.openschema.mma.data.MetricsDAO;
import io.openschema.mma.data.MetricsEntity;
import io.openschema.mma.networking.BackendApi;

/**
 * Repository class to manage the metrics data.
 */
class MetricsRepository {

    private static final String TAG = "MetricsRepository";

    //Singleton
    private static MetricsRepository _instance = null;

    /**
     * Call to retrieve a {@link MetricsRepository} object.
     */
    public static MetricsRepository getRepository(Context appContext) {
        Log.d(TAG, "UI: Fetching MetricsRepository");
        if (_instance == null) {
            synchronized (BackendApi.class) {
                if (_instance == null) {
                    _instance = new MetricsRepository(appContext);
                }
            }
        }
        return _instance;
    }

    /**
     * Thread pool used to handle multiple metrics being pushed to the database simultaneously.
     */
    private final ThreadPoolExecutor mExecutor;

    /**
     * Data access object used to interact with the Metrics' table in the database.
     */
    private final MetricsDAO mMetricsDAO;

    private MetricsRepository(Context appContext) {
        MMADatabase db = MMADatabase.getDatabase(appContext);
        mMetricsDAO = db.metricsDAO();
        mExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    }

    /**
     * Writes a metrics object to the database. Queued metrics will get flushed periodically through {@link MetricsWorker}.
     */
    public void queueMetric(MetricsEntity metricsEntity) {
        mExecutor.execute(() -> mMetricsDAO.insert(metricsEntity));
    }

    /**
     * Retrieves a list of all currently queued metrics.
     */
    public List<MetricsEntity> getEnqueuedMetrics() {
        return mMetricsDAO.getAll();
    }

    /**
     * Deletes metrics that have been recently pushed by the {@link MetricsWorker}.
     *
     * @param metrics List of metrics to delete from the database
     */
    public void clearMetrics(List<MetricsEntity> metrics) {
        mMetricsDAO.delete(metrics.toArray(new MetricsEntity[0]));
    }
}
