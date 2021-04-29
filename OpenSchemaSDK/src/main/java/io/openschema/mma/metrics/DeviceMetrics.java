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
import android.net.wifi.WifiManager;
import android.os.Build;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.util.List;

import androidx.core.util.Pair;
import io.openschema.mma.id.UUID;

/**
 * Collects metrics related to the device.
 */
public class DeviceMetrics extends BaseMetrics {
    private static final String TAG = "DeviceMetrics";

    /**
     * Metric family name to be used for the collected device information.
     */
    public static final String METRIC_FAMILY_NAME = "openschema_android_device_info";

    private static final String METRIC_OS_VERSION = "os_version";
    private static final String METRIC_MODEL = "model";
    private static final String METRIC_MANUFACTURER = "manufacturer";
    private static final String METRIC_BRAND = "brand";
    private static final String METRIC_ANDROID_ID = "android_id";
    private static final String METRIC_OPENSCHEMA_VERSION = "openschema_version"; //TODO: need to decide on a versioning scheme

    private String mSSAID;

    public DeviceMetrics(Context context) {
        super(context);
        mSSAID = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
    }

    /**
     * Collects information about the device and generates a list of pairs to
     * be used in {@link MetricsManager#collect(String, List)}.
     */
    public List<Pair<String, String>> retrieveDeviceMetrics() {
        Log.d(TAG, "MMA: Generating device metrics...");

        List<Pair<String, String>> metricsList = generateTimeZoneMetrics();

        metricsList.add(new Pair<>(METRIC_OS_VERSION, Integer.toString(Build.VERSION.SDK_INT)));
        metricsList.add(new Pair<>(METRIC_MODEL, Build.MODEL));
        metricsList.add(new Pair<>(METRIC_MANUFACTURER, Build.MANUFACTURER));
        metricsList.add(new Pair<>(METRIC_BRAND, Build.BRAND));
        metricsList.add(new Pair<>(METRIC_ANDROID_ID, mSSAID));

        Log.d(TAG, "MMA: Collected metrics:\n" + metricsList.toString());
        return metricsList;
    }
}
