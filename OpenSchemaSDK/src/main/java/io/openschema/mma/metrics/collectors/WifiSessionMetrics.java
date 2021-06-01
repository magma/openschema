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
import android.net.NetworkCapabilities;

//TODO: javadocs
public class WifiSessionMetrics extends NetworkSessionMetrics {

    /**
     * Metric name to be used for the collected information.
     */
    public static final String METRIC_NAME = "openschemaWifiSession";

    public WifiSessionMetrics(Context context, MetricsCollectorListener listener) {
        super(context, METRIC_NAME, NetworkCapabilities.TRANSPORT_WIFI, new WifiNetworkMetrics(context), listener);
    }
}