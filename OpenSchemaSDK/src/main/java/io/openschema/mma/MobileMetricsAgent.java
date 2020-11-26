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

import io.openschema.mma.bootstrap.BootstrapManager;
import io.openschema.mma.id.Identity;
import io.openschema.mma.networking.BackendApi;
import io.openschema.mma.networking.RetrofitService;
import io.openschema.mma.register.RegistrationManager;

/**
 * Main class to act as an interface to access the functionality in the library.
 */
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
    private BootstrapManager mBootstrapManager;

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

    /**
     * Initialize the object using the parameters supplied by the {@link Builder Builder}
     * <p>
     * This call will register the device with a unique UUID to the cloud, if it hasn't been registered yet,
     * and then proceed to execute the bootstrapping sequence. The {@link MobileMetricsAgent} needs to be
     * initialized before attempting to push information to the data lake.
     *
     * @throws CertificateException
     * @throws NoSuchAlgorithmException
     * @throws KeyStoreException
     * @throws NoSuchProviderException
     * @throws InvalidAlgorithmParameterException
     * @throws IOException
     */
    public void init() throws CertificateException, NoSuchAlgorithmException, KeyStoreException, NoSuchProviderException, InvalidAlgorithmParameterException, IOException {
        Log.d(TAG, "MMA: Initializing MMA...");
        Identity identity = new Identity(mAppContext);

        RetrofitService mRetrofitService = RetrofitService.getService(mAppContext);
        mRetrofitService.initApi(mAppContext, mBackendBaseURL, mBackendCertificateResId, mBackendUsername, mBackendPassword);
        BackendApi mBackendApi = mRetrofitService.getApi();

        mRegistrationManager = new RegistrationManager(mAppContext, identity, mBackendApi);
        mBootstrapManager = new BootstrapManager(mAppContext, mBootstrapperCertificateResId, identity);

        //TODO: spawn a background thread instead? Bootstrapping process is currently blocking the main thread
        mRegistrationManager.setOnRegisterListener(() -> {
            try {
                mBootstrapManager.bootstrapNow(mBootstrapperAddress, mControllerPort);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        //TODO: Consider checking whether uuid is already saved in sharedprefs instead of sending a new request
        mRegistrationManager.register();
    }


    /**
     * Builder class for {@link MobileMetricsAgent} objects.
     *
     * <p>Example:
     *
     *     <pre>
     *     MobileMetricsAgent mma = new MobileMetricsAgent.Builder()
     *             .setAppContext(getApplicationContext())
     *             .setControllerAddress(getString(R.string.controller_address))
     *             .setControllerPort(getResources().getInteger(R.integer.controller_port))
     *             .setBootstrapperAddress(getString(R.string.bootstrapper_address))
     *             .setBootstrapperCertificateResId(R.raw.bootstrap)
     *             .setAuthorityHeader(getString(R.string.metrics_authority_header))
     *             .setBackendBaseURL(getString(R.string.backend_base_url))
     *             .setBackendCertificateResId(R.raw.server)
     *             .setBackendUsername(getString(R.string.backend_username))
     *             .setBackendPassword(getString(R.string.backend_password))
     *             .build();
     *     </pre>
     */
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


        /**
         * @param address URL of Magma's controller
         * @return
         */
        public Builder setControllerAddress(String address) {
            mControllerAddress = address;
            return this;
        }

        /**
         * @param address URL of the bootstrapper's controller
         * @return
         */
        public Builder setBootstrapperAddress(String address) {
            mBootstrapperAddress = address;
            return this;
        }

        /**
         * @param certificateResId Resource ID of the bootstrapper's controller raw certificate
         * @return
         */
        public Builder setBootstrapperCertificateResId(int certificateResId) {
            mBootstrapperCertificateResId = certificateResId;
            return this;
        }

        /**
         * @param address URL of the metrics' controller
         * @return
         */
        public Builder setAuthorityHeader(String address) {
            mMetricsAuthorityHeader = address;
            return this;
        }

        /**
         * @param port Port used by the bootstrapper's controller
         * @return
         */
        public Builder setControllerPort(int port) {
            mControllerPort = port;
            return this;
        }

        /**
         * @param baseURL Base URL of OpenSchema's middle box
         * @return
         */
        public Builder setBackendBaseURL(String baseURL) {
            mBackendBaseURL = baseURL;
            return this;
        }

        /**
         * @param certificateResId Resource ID of the OpenSchema's middle box raw certificate
         * @return
         */
        public Builder setBackendCertificateResId(int certificateResId) {
            mBackendCertificateResId = certificateResId;
            return this;
        }

        /**
         * @param backendUsername Secret username to access OpenSchema's middle box Basic Auth
         * @return
         */
        public Builder setBackendUsername(String backendUsername) {
            mBackendUsername = backendUsername;
            return this;
        }

        /**
         * @param backendPassword Secret password to access OpenSchema's middle box Basic Auth
         * @return
         */
        public Builder setBackendPassword(String backendPassword) {
            mBackendPassword = backendPassword;
            return this;
        }

        /**
         * @param context Application context
         * @return
         */
        public Builder setAppContext(Context context) {
            mAppContext = context;
            return this;
        }

        /**
         * Combine the supplied options and return a {@link MobileMetricsAgent} object.
         */
        public MobileMetricsAgent build() {
            return new MobileMetricsAgent(this);
        }
    }
}
