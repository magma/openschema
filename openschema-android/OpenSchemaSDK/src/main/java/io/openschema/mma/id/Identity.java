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

import android.content.Context;
import android.util.Log;

/**
 * Class used by {@link io.openschema.mma.MobileMetricsAgent MobileMetricsAgent} to share the UUID for registration and metrics pushing.
 */
public class Identity {

    private String mUUID;

    /**
     * Instantiate a new {@link Identity} object and generate new UUID and public key values.
     */
    public Identity(Context context) {
        mUUID = new UUID(context).getUUID();

        //TODO: remove, used for testing purposes. Add API to retrieve UUID on UI (about page)
        Log.d("MMA: UUID: ", mUUID);
    }


    /**
     * Get the object's UUID value
     */
    public String getUUID() {
        return mUUID;
    }
}
