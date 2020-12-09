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

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import io.openschema.mma.metricsd.MetricsContainer;
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
     * Queue that holds the metrics pending to be sent to the controller.
     */
    //TODO: Change the queue to hold MetricFamily objects instead for optimization.
    private final Queue<MetricsContainer> mMetricsQueue = new ConcurrentLinkedQueue<>();

    private MetricsRepository(Context appContext) {
    }

    /**
     * Adds a metrics object to the queue. This queue gets flushed periodically through {@link MetricsWorker}.
     */
    //TODO: Add data persistence. Currently the data is held in memory and killing the app
    // would cause unsent metrics to be lost.
    public void queueMetric(MetricsContainer metricsContainer) {
        mMetricsQueue.add(metricsContainer);
    }

    /**
     * Retrieves the current metrics queue.
     */
    public Queue<MetricsContainer> getQueue() {
        return mMetricsQueue;
    }
}
