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

package io.openschema.mma.id;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;

/**
 * Helper class used to generate and store a UUID.
 */

public class UUID {

    private static final String KEY_UUID = "key_uuid";

    private String mUUID;

    public UUID(Context context) {
        mUUID = generateUUID(context);
    }

    @SuppressLint("ApplySharedPref")
    private String generateUUID(Context context) {
        String uuid;

        //Load saved value from shared preferences
        SharedPreferences sharedPref = context.getSharedPreferences(
                KEY_UUID, Context.MODE_PRIVATE);
        uuid = sharedPref.getString(KEY_UUID, null);

        //Generate new UUID if none was found
        if (uuid == null) {
            uuid = java.util.UUID.randomUUID().toString();
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putString(KEY_UUID, uuid);
            editor.commit();
        }

        return uuid;
    }

    public String getUUID() {
        return mUUID;
    }

    public void setUUID(String uuid) {
        mUUID = uuid;
    }

}