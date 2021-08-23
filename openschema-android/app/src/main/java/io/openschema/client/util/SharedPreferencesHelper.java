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

package io.openschema.client.util;

import android.content.Context;
import android.content.SharedPreferences;

import io.openschema.client.BuildConfig;

/**
 * Helper class to receive a SharedPreferences object built using the same
 * file key and keep the preferences' keys in a single place.
 */
public class SharedPreferencesHelper {

    private static final String PREFERENCE_FILE_KEY = BuildConfig.APPLICATION_ID + ".PREFERENCE_FILE_KEY";

    /**
     * Key used to save whether the Terms of Service and Privacy Policy was accepted.
     */
    public static final String KEY_TOS_ACCEPTED = "key_tos_accepted";

    /**
     * Get a SharedPreferences instance using the app's file key.
     */
    public static SharedPreferences getInstance(Context context) {
        return context.getSharedPreferences(PREFERENCE_FILE_KEY, Context.MODE_PRIVATE);
    }
}
