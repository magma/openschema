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

package io.openschema.mma.register;

import android.util.Log;

import java.io.IOException;

import androidx.annotation.WorkerThread;
import io.openschema.mma.id.Identity;
import io.openschema.mma.networking.BackendApi;
import io.openschema.mma.networking.request.RegisterRequest;
import io.openschema.mma.networking.response.BaseResponse;
import retrofit2.Response;

/**
 * Class in charge of registering the UE using a generated UUID and Key.
 */
public class RegistrationManager {

    private static final String TAG = "RegistrationManager";

    private Identity mIdentity;
    private BackendApi mBackendApi;

    public RegistrationManager(BackendApi backendApi, Identity identity) {
        mBackendApi = backendApi;
        mIdentity = identity;
    }

    /**
     * Sends a request to register the UE as a gateway in the Magma cloud. If the UE has
     * already been registered, the server will respond with 409. This operation can't be
     * called from the main thread.
     */
    @WorkerThread
    public boolean registerSync() {
        Log.d(TAG, "MMA: Sending registration request.");
        try {
            Response<BaseResponse> res = mBackendApi.register(new RegisterRequest(mIdentity.getUUID(), mIdentity.getPublicKey()))
                    .execute();

            if (res.isSuccessful()) {
                Log.d(TAG, "MMA: onResponse success: " + res.body().getMessage());
                Log.d(TAG, "MMA: UE registration was successful.");
                return true;
            } else {
                String errorMessage = BaseResponse.getErrorMessage(res.errorBody());
                Log.d(TAG, "MMA: onResponse failure (" + res.code() + "): " + errorMessage);

                //If the user is already registered, proceed to bootstrapping as normal
                if (res.code() == 409) {
                    return true;
                }
            }
        } catch (IOException e) {
            Log.d(TAG, "MMA: Failure talking with the server");
            e.printStackTrace();
        }

        return false;
    }
}
