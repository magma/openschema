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
import io.openschema.mma.data.MetricsEntity;

/**
 * Class in charge of handling pushing metrics to the controller.
 */
public class MetricsManager {

    private static final String TAG = "MetricsManager";

    public static final String METRIC_UUID = "uuid";
    public static final String METRIC_TIMESTAMP = "timestamp";

    private final MetricsRepository mMetricsRepository;

    /**
     * Constructs the manager class and initializes {@link MetricsWorker} to run periodically.
     */
    public MetricsManager(Context appContext) {
        mMetricsRepository = MetricsRepository.getRepository(appContext);
    }

    /**
     * Send metrics to prometheus through GRPC using the Collect method in metricsd.proto
     *
     * @param metricName Root name for the group of collected metrics
     * @param metrics    List of metrics to collect with the <name, value> structure
     */
    public void collect(String metricName, List<Pair<String, String>> metrics) {
        Log.d(TAG, "MMA: Collecting metric \"" + metricName + "\"");

        collect(new MetricsEntity(metricName, Long.toString(System.currentTimeMillis()), metrics));
    }

    /**
     * Sends the metrics object to the repository to be stored for batching.
     */
    private void collect(MetricsEntity metricsEntity) {
        mMetricsRepository.queueMetric(metricsEntity);
    }

    //TODO: javadoc
    public static void startWorker(Context appContext, String metricsControllerAddress, String bootstrapperAddress, int metricsControllerPort, String metricsAuthorityHeader) {
        //Start the background worker to periodically push saved metrics.
        MetricsWorker.enqueuePeriodicWorker(appContext, metricsControllerAddress, bootstrapperAddress, metricsControllerPort, metricsAuthorityHeader);
    }
}
