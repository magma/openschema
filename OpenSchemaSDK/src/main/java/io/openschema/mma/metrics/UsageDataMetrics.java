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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import androidx.core.util.Pair;
import io.openschema.mma.helpers.UsageRetriever;

//TODO: javadocs
public class UsageDataMetrics {
    private static final String TAG = "DeviceMetrics";

    public static final String METRIC_FAMILY_NAME = "openschema_android_usage_data";

    private static final String METRIC_CURRENT_HOUR = "current_hour";
    private static final String METRIC_WIFI_USAGE = "wifi_usage";
    private static final String METRIC_CELLULAR_USAGE = "cellular_usage";

    private final UsageRetriever mUsageRetriever;

    public UsageDataMetrics(Context context) {
        mUsageRetriever = new UsageRetriever(context);
    }

    public List<Pair<String, String>> retrieveHourUsage(Calendar currentHourCalendar) {
        Log.d(TAG, String.format("MMA: Generating usage data metrics for %s...", currentHourCalendar.getTime().toString()));

        List<Pair<String, String>> metricsList = new ArrayList<>();

        long startTime = currentHourCalendar.getTimeInMillis();
        metricsList.add(new Pair<>(METRIC_CURRENT_HOUR, Long.toString(startTime)));

        currentHourCalendar.add(Calendar.HOUR_OF_DAY, 1);
        long endTime = currentHourCalendar.getTimeInMillis();

        //TODO: fetch usage data for wifi & cellular
        metricsList.add(new Pair<>(METRIC_WIFI_USAGE, Long.toString(mUsageRetriever.getDeviceWifiTonnage(startTime, endTime))));
        metricsList.add(new Pair<>(METRIC_CELLULAR_USAGE, Long.toString(mUsageRetriever.getDeviceCellularTonnage(startTime, endTime))));

        Log.d(TAG, "MMA: Collected metrics:\n" + metricsList.toString());
        return metricsList;
    }
}
