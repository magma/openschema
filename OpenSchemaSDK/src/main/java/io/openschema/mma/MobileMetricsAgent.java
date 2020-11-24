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

package io.openschema.mma;

import android.content.Context;
import android.util.Log;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.cert.CertificateException;

import io.openschema.mma.bootstrap.BootStrapManager;
import io.openschema.mma.id.Identity;
import io.openschema.mma.networking.BackendApi;
import io.openschema.mma.networking.RetrofitService;
import io.openschema.mma.register.RegistrationManager;

public class MobileMetricsAgent {

    private static final String TAG = "MobileMetricsAgent";

    private String mControllerAddress;
    private String mBootstrapperAddress;
    private int mBootstrapperCertificateResId;
    private String mMetricsAuthorityHeader;
    private int mControllerPort;

    private String mBackendBaseURL;
    private int mBackendCertificateResId;
    private String mBackendUsername;
    private String mBackendPassword;

    private Context mAppContext;
    private RegistrationManager mRegistrationManager;
    private BootStrapManager mBootStrapManager;

    private MobileMetricsAgent(Builder mmaBuilder) {
        mControllerAddress = mmaBuilder.mControllerAddress;
        mBootstrapperAddress = mmaBuilder.mBootstrapperAddress;
        mBootstrapperCertificateResId = mmaBuilder.mBootstrapperCertificateResId;
        mMetricsAuthorityHeader = mmaBuilder.mMetricsAuthorityHeader;
        mControllerPort = mmaBuilder.mControllerPort;

        mBackendBaseURL = mmaBuilder.mBackendBaseURL;
        mBackendCertificateResId = mmaBuilder.mBackendCertificateResId;
        mBackendUsername = mmaBuilder.mBackendUsername;
        mBackendPassword = mmaBuilder.mBackendPassword;

        mAppContext = mmaBuilder.mAppContext;
    }

    //Will register the UE & run the bootstrap process for GRPC
    public void init() throws CertificateException, NoSuchAlgorithmException, KeyStoreException, NoSuchProviderException, InvalidAlgorithmParameterException, IOException {
        Log.d(TAG, "MMA: Initializing MMA...");
        Identity identity = new Identity(mAppContext);

        RetrofitService mRetrofitService = RetrofitService.getService(mAppContext);
        mRetrofitService.initApi(mAppContext, mBackendBaseURL, mBackendCertificateResId, mBackendUsername, mBackendPassword);
        BackendApi mBackendApi = mRetrofitService.getApi();

        mRegistrationManager = new RegistrationManager(mAppContext, identity, mBackendApi);
        mBootStrapManager = new BootStrapManager(mAppContext, mBootstrapperCertificateResId, identity);

        //TODO: spawn a background thread instead? Bootstrapping process is currently blocking the main thread
        mRegistrationManager.setOnRegisterListener(() -> {
            try {
                mBootStrapManager.bootstrapNow(mBootstrapperAddress, mControllerPort);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        //TODO: Consider checking whether uuid is already saved in sharedprefs instead of sending a new request
        mRegistrationManager.register();
    }

    public String getControllerAddress() {
        return mControllerAddress;
    }

    public String getBoostStrapperAddress() {
        return mBootstrapperAddress;
    }

    public String getMetricsAuthorityHeader() {
        return mMetricsAuthorityHeader;
    }

    public int getControllerPort() {
        return mControllerPort;
    }


    public static class Builder {
        private String mControllerAddress;
        private String mBootstrapperAddress;
        private int mBootstrapperCertificateResId;
        private String mMetricsAuthorityHeader;
        private int mControllerPort;

        private String mBackendBaseURL;
        private int mBackendCertificateResId;
        private String mBackendUsername;
        private String mBackendPassword;

        private Context mAppContext;


        public Builder setControllerAddress(String address) {
            mControllerAddress = address;
            return this;
        }

        public Builder setBootstrapperAddress(String address) {
            mBootstrapperAddress = address;
            return this;
        }

        public Builder setBootstrapperCertificateResId(int certificateResId) {
            mBootstrapperCertificateResId = certificateResId;
            return this;
        }

        public Builder setAuthorityHeader(String address) {
            mMetricsAuthorityHeader = address;
            return this;
        }

        public Builder setControllerPort(int port) {
            mControllerPort = port;
            return this;
        }

        public Builder setBackendBaseURL(String baseURL) {
            mBackendBaseURL = baseURL;
            return this;
        }

        public Builder setBackendCertificateResId(int certificateResId) {
            mBackendCertificateResId = certificateResId;
            return this;
        }

        public Builder setBackendUsername(String backendUsername) {
            mBackendUsername = backendUsername;
            return this;
        }

        public Builder setBackendPassword(String backendPassword) {
            mBackendPassword = backendPassword;
            return this;
        }

        public Builder setAppContext(Context context) {
            mAppContext = context;
            return this;
        }

        public MobileMetricsAgent build() {
            return new MobileMetricsAgent(this);
        }
    }
}
