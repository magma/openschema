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

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import androidx.core.util.Pair;

/**
 * TODO: javadocs
 */
public abstract class BaseMetrics {
    private static final String TAG = "BaseMetrics";

    protected static final String METRIC_UTC_TIME = "utc_time";
    protected static final String METRIC_TIME_ZONE_OFFSET_MILLIS = "timezone_offset_millis";
    protected static final String METRIC_TIME_ZONE_NAME = "timezone_name";

    public BaseMetrics(Context context) { }

    public List<Pair<String, String>> generateTimeZoneMetrics() {
        Log.d(TAG, "MMA: Generating time metrics...");
        List<Pair<String, String>> metricsList = new ArrayList<>();

        DateFormat utcDateFormat = DateFormat.getTimeInstance();
        utcDateFormat.setTimeZone(TimeZone.getTimeZone("gmt"));

        TimeZone currentTimeZone = TimeZone.getDefault();

        metricsList.add(new Pair<>(METRIC_UTC_TIME, utcDateFormat.format(new Date())));
        metricsList.add(new Pair<>(METRIC_TIME_ZONE_OFFSET_MILLIS, Integer.toString(currentTimeZone.getRawOffset())));
        metricsList.add(new Pair<>(METRIC_TIME_ZONE_NAME, currentTimeZone.getDisplayName()));
        return metricsList;
    }

    public abstract List<Pair<String, String>> retrieveMetrics();

    public interface MetricsCollectorListener {
        void onMetricCollected(String metricName, List<Pair<String, String>> metricsList);
    }
}
