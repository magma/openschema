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
import io.openschema.mma.data.Timestamp;

/**
 * Class in charge of handling pushing metrics to the controller.
 */
public class MetricsManager {

    private static final String TAG = "MetricsManager";

    private final MetricsRepository mMetricsRepository;

    /**
     * Constructs the manager class and initializes {@link MetricsWorker} to run periodically.
     */
    public MetricsManager(Context appContext) {
        mMetricsRepository = MetricsRepository.getRepository(appContext);
    }

    /**
     * Sends the metrics object to the repository to be stored for batching.
     *
     * @param metricName  Root name for the group of collected metricsList
     * @param metricsList List of metricsList to collect with the <name, value> structure
     */
    public void collect(String metricName, List<Pair<String, String>> metricsList) {
        Log.d(TAG, "MMA: Collecting metric \"" + metricName + "\"");
        collect(new MetricsEntity(metricName, metricsList, Timestamp.getTimestampInstance()));
    }

    /**
     * Sends the metrics object to the repository to be stored for batching.
     */
    private void collect(MetricsEntity metricsEntity) {
        mMetricsRepository.queueMetric(metricsEntity);
    }

    //TODO: javadoc
    public static void startWorker(Context appContext, String backendUrl, String backendUsername, String backendPassword) {
        //Start the background worker to periodically push saved metrics.
        MetricsWorker.enqueuePeriodicWorker(appContext, backendUrl, backendUsername, backendPassword);
    }
}
