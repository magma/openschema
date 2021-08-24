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

package io.openschema.mma.metrics.collectors;

import android.content.Context;

import java.util.List;

import androidx.core.util.Pair;

/**
 * Base class used metrics that require asynchronous computation to collect
 */
public abstract class AsyncMetrics extends BaseMetrics {
    private static final String TAG = "AsyncMetrics";

    public AsyncMetrics(Context context) {
        super(context);
    }

    /**
     * Interface used in metrics that are collected asynchronously and require a callback.
     */
    public interface MetricsCollectorListener {
        void onMetricCollected(String metricName, List<Pair<String, String>> metricsList);
    }
}
