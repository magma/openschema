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

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.io.IOException;

import androidx.annotation.WorkerThread;
import io.openschema.mma.utils.SharedPreferencesHelper;
import io.openschema.mma.id.Identity;
import io.openschema.mma.backend.BackendApi;
import io.openschema.mma.backend.request.RegisterRequest;
import io.openschema.mma.backend.response.BaseResponse;
import retrofit2.Response;

/**
 * DEPRECATED. Might get reused in the future for a more robust device registration into data lake.
 * Class in charge of registering the UE using a generated UUID and Key.
 */
public class RegistrationManager {

    private static final String TAG = "RegistrationManager";

    private Identity mIdentity;
    private BackendApi mBackendApi;
    private SharedPreferences mSharedPreferences;

    public RegistrationManager(Context context, BackendApi backendApi, Identity identity) {
        mSharedPreferences = SharedPreferencesHelper.getInstance(context);
        mBackendApi = backendApi;
        mIdentity = identity;
    }

    /**
     * Sends a request to register the UE as a gateway in the Magma cloud. If the UE has
     * already been registered, the server will respond with 409. This operation can't be
     * called from the main thread.
     * @return Returns true if the UE was registered successfully.
     */
    @WorkerThread
    public boolean registerSync() {

        boolean isRegistered = mSharedPreferences.getBoolean(SharedPreferencesHelper.KEY_UE_REGISTERED, false);

        if (isRegistered) {
            Log.d(TAG, "MMA: UE has already been registered, no request will be sent.");
            return true;
        }

        Log.d(TAG, "MMA: Sending registration request.");
        try {
            Response<BaseResponse> res = mBackendApi.register(new RegisterRequest(mIdentity.getUUID()))
                    .execute();

            if (res.isSuccessful()) {
                Log.d(TAG, "MMA: onResponse success: " + res.body().getMessage());
                Log.d(TAG, "MMA: UE registration was successful.");
                saveRegistration();
                return true;
            } else {
                String errorMessage = BaseResponse.getErrorMessage(res.errorBody());
                Log.d(TAG, "MMA: onResponse failure (" + res.code() + "): " + errorMessage);

                //If the user is already registered, proceed to bootstrapping as normal
                if (res.code() == 409) {
                    saveRegistration();
                    return true;
                }
            }
        } catch (IOException e) {
            Log.d(TAG, "MMA: Failure talking with the server");
            e.printStackTrace();
        }

        return false;
    }

    /**
     * Set the registration flag to true in SharedPreferences to avoid further requests in the future.
     */
    private void saveRegistration() {
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putBoolean(SharedPreferencesHelper.KEY_UE_REGISTERED, true);
        editor.apply();
    }
}
