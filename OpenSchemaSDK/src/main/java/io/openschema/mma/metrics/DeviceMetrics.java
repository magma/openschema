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
import android.os.Build;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import androidx.core.util.Pair;

/**
 * Collects metrics related to the device.
 */
public class DeviceMetrics {
    private static final String TAG = "DeviceMetrics";

    /**
     * Metric family name to be used for the collected device information.
     */
    public static final String METRIC_FAMILY_NAME = "openschema_android_device_info";

    private static final String METRIC_MODEL = "model";
    private static final String METRIC_OS_VERSION = "os_version";

    public DeviceMetrics(Context context) { }

    /**
     * Collects information about the device and generates a list of pairs to
     * be used in {@link MetricsManager#collect(String, List)}.
     */
    public List<Pair<String, String>> retrieveDeviceMetrics() {
        Log.d(TAG, "MMA: Generating device metrics...");

        List<Pair<String, String>> metricsList = new ArrayList<>();

        metricsList.add(new Pair<>(METRIC_MODEL, Build.MODEL));
        metricsList.add(new Pair<>(METRIC_OS_VERSION, Integer.toString(Build.VERSION.SDK_INT)));

        Log.d(TAG, "MMA: Collected metrics:\n" + metricsList.toString());
        return metricsList;
    }
}
