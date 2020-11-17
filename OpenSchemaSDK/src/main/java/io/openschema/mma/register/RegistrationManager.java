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
 * Client or UE need to be registered on the Cloud with their UUID and Key for the bootstrapping process.
 * Registration is manual now but should be automated.
 * <p>
 * Sample Service
 * message UeParams {
 * bytes key = 1;
 * string uuid = 2;
 * }
 * service RegisterUE {
 * rpc Register (UeParams) returns (Response) {}
 * }
 * message Response {
 * string response = 1;
 * }
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

    //Send POST request to register the UE as a gateway in magma
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

    public interface OnRegisterListener {
        void OnRegister();
    }
}
