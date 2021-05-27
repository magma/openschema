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
import android.provider.Settings;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import androidx.core.util.Pair;

/**
 * Collects metrics related to the device.
 */
public class DeviceMetrics extends BaseMetrics {
    private static final String TAG = "DeviceMetrics";

    /**
     * Metric name to be used for the collected information.
     */
    public static final String METRIC_NAME = "openschemaDeviceInfo";

    private static final String METRIC_OS_VERSION = "osVersion";
    private static final String METRIC_MODEL = "model";
    private static final String METRIC_MANUFACTURER = "manufacturer";
    private static final String METRIC_BRAND = "brand";
    private static final String METRIC_ANDROID_ID = "androidId";
    private static final String METRIC_OPENSCHEMA_VERSION = "openschemaVersion"; //TODO: need to decide on a versioning scheme

    private String mSSAID;

    public DeviceMetrics(Context context) {
        super(context);
        mSSAID = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
    }

    /**
     * Collects information about the device and generates a list of pairs to
     * be used in {@link MetricsManager#collect(String, List)}.
     */
    public List<Pair<String, String>> retrieveMetrics() {
        Log.d(TAG, "MMA: Generating device metrics...");

        List<Pair<String, String>> metricsList = new ArrayList<>();

        metricsList.add(new Pair<>(METRIC_OS_VERSION, Integer.toString(Build.VERSION.SDK_INT)));
        metricsList.add(new Pair<>(METRIC_MODEL, Build.MODEL));
        metricsList.add(new Pair<>(METRIC_MANUFACTURER, Build.MANUFACTURER));
        metricsList.add(new Pair<>(METRIC_BRAND, Build.BRAND));
        metricsList.add(new Pair<>(METRIC_ANDROID_ID, mSSAID));

        //TODO: Add debugging flag to enable detailed metrics
        Log.d(TAG, "MMA: Collected metrics:\n" + metricsList.toString());
        return metricsList;
    }
}
