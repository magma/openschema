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

package io.openschema.mma.utils;

import android.content.Context;
import android.content.SharedPreferences;

import io.openschema.mma.BuildConfig;

/**
 * Helper class to receive a SharedPreferences object built using the same
 * file key and keep the preferences' keys in a single place.
 */
public class SharedPreferencesHelper {

    private static final String PREFERENCE_FILE_KEY = BuildConfig.LIBRARY_PACKAGE_NAME + ".PREFERENCE_FILE_KEY";

    /**
     * Key used to save the UUID string.
     */
    public static final String KEY_UUID = "key_uuid";

    /**
     * Key used to keep track whether the UE has been registered already.
     */
    public static final String KEY_UE_REGISTERED = "key_ue_registered";

    /**
     * Key used to keep track of the hours that have had their usage collected by HourlyUsageWorker.
     */
    public static final String KEY_LAST_HOURLY_USAGE_TIMESTAMP = "key_last_hourly_usage_timestamp";

    /**
     * Get a SharedPreferences instance using the library's file key.
     */
    public static SharedPreferences getInstance(Context context) {
        return context.getSharedPreferences(PREFERENCE_FILE_KEY, Context.MODE_PRIVATE);
    }
}
