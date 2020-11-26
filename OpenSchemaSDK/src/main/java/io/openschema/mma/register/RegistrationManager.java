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
import android.util.Log;

import io.openschema.mma.id.Identity;
import io.openschema.mma.networking.BackendApi;
import io.openschema.mma.networking.request.RegisterRequest;
import io.openschema.mma.networking.response.BaseResponse;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Class in charge of registering the UE using a generated UUID and Key.
 */
public class RegistrationManager {

    private static final String TAG = "RegistrationManager";

    private Identity mIdentity;
    private BackendApi mBackendApi;

    private OnRegisterListener mListener = null;

    public RegistrationManager(Context context, Identity identity, BackendApi backendApi) {
        mIdentity = identity;
        mBackendApi = backendApi;
    }

    /**
     * Sends a POST request to register the UE as a gateway in the Magma cloud. If the UE has
     * already been registered, no request will be sent.
     */
    public void register() {
        Log.d(TAG, "MMA: Sending registration request.");
        mBackendApi.register(new RegisterRequest(mIdentity.getUUID(), mIdentity.getPublicKey()))
                .enqueue(new Callback<BaseResponse>() {
                    @Override
                    public void onResponse(Call<BaseResponse> call, Response<BaseResponse> res) {
                        if (res.isSuccessful() && mListener != null) {
                            Log.d(TAG, "MMA: onResponse success: " + res.body().getMessage());
                            Log.d(TAG, "MMA: UE registration was successful.");
                            mListener.OnRegister();
                        } else {
                            String errorMessage = BaseResponse.getErrorMessage(res.errorBody());
                            Log.d(TAG, "MMA: onResponse failure (" + res.code() + "): " + errorMessage);

                            //If the user is already registered, proceed to bootstrapping as normal
                            if (res.code() == 409) {
                                mListener.OnRegister();
                            }
                        }
                    }
                    @Override
                    public void onFailure(Call<BaseResponse> call, Throwable t) {
                        Log.d(TAG, "MMA: onFailure: " + t.toString());
                    }
                });
    }

    public void setOnRegisterListener(OnRegisterListener listener) {
        mListener = listener;
    }

    /**
     * Interface with a callback to be invoked when the OpenSchema middle box responds with
     * a successful registration. It will also be called if it detects that the UE was already
     * registered.
     */
    public interface OnRegisterListener {
        void OnRegister();
    }
}
