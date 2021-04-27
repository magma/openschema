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

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.cert.CertificateException;
import java.util.List;

import androidx.core.util.Pair;
import io.openschema.mma.bootstrap.BootstrapManager;
import io.openschema.mma.certifier.Certificate;
import io.openschema.mma.helpers.SharedPreferencesHelper;
import io.openschema.mma.id.Identity;
import io.openschema.mma.metrics.CellularNetworkMetrics;
import io.openschema.mma.metrics.DeviceMetrics;
import io.openschema.mma.metrics.MetricsManager;
import io.openschema.mma.metrics.WifiNetworkMetrics;
import io.openschema.mma.networking.CertificateManager;
import io.openschema.mma.networking.RetrofitService;
import io.openschema.mma.register.RegistrationManager;

/**
 * Main class to act as an interface to access the functionality in the library.
 */
public class MobileMetricsAgent {

    private static final String TAG = "MobileMetricsAgent";

    private String mControllerAddress;
    private String mBootstrapperAddress;
    private int mControllerCertificateResId;
    private String mMetricsAuthorityHeader;
    private int mControllerPort;
    private boolean mUseAutomaticRegistration;
    private String mBackendBaseURL;
    private int mBackendCertificateResId;
    private String mBackendUsername;
    private String mBackendPassword;
    private boolean mEnableLibraryMetrics;

    private Context mAppContext;
    private Identity mIdentity;
    private CertificateManager mCertificateManager;
    private boolean mIsReady = false;

    private RegistrationManager mRegistrationManager = null;
    private BootstrapManager mBootstrapManager = null;
    private MetricsManager mMetricsManager = null;

    private MobileMetricsAgent(Builder mmaBuilder) {
        mControllerAddress = mmaBuilder.mControllerAddress;
        mBootstrapperAddress = mmaBuilder.mBootstrapperAddress;
        mControllerCertificateResId = mmaBuilder.mControllerCertificateResId;
        mMetricsAuthorityHeader = mmaBuilder.mMetricsAuthorityHeader;
        mControllerPort = mmaBuilder.mControllerPort;
        mUseAutomaticRegistration = mmaBuilder.mUseAutomaticRegistration;
        mBackendBaseURL = mmaBuilder.mBackendBaseURL;
        mBackendCertificateResId = mmaBuilder.mBackendCertificateResId;
        mBackendUsername = mmaBuilder.mBackendUsername;
        mBackendPassword = mmaBuilder.mBackendPassword;
        mEnableLibraryMetrics = mmaBuilder.mEnableLibraryMetrics;

        mAppContext = mmaBuilder.mAppContext;
    }

    /**
     * Initialize the object using the parameters supplied by the {@link Builder Builder}
     * <p>
     * This call will register the device with a unique UUID to the cloud, if it hasn't been registered yet,
     * and then proceed to execute the bootstrapping sequence. The {@link MobileMetricsAgent} needs to be
     * initialized before attempting to push information to the data lake.
     */
    public void init() throws CertificateException, NoSuchAlgorithmException, KeyStoreException, NoSuchProviderException, InvalidAlgorithmParameterException, IOException {
        Log.d(TAG, "MMA: Initializing MMA...");

        //Initialize identity & certificates
        mIdentity = new Identity(mAppContext);
        mCertificateManager = new CertificateManager();
        mCertificateManager.addBackendCertificate(mAppContext, mBackendCertificateResId);
        mCertificateManager.addControllerCertificate(mAppContext, mControllerCertificateResId);

        //Initialize automatic registration managers if enabled
        if (mUseAutomaticRegistration) {
            RetrofitService retrofitService = RetrofitService.getService(mAppContext);
            retrofitService.initApi(mBackendBaseURL, mCertificateManager.generateSSLContext(), mBackendUsername, mBackendPassword);
            mRegistrationManager = new RegistrationManager(mAppContext, retrofitService.getApi(), mIdentity);
        }

        //Initialize managers
        mBootstrapManager = new BootstrapManager(mBootstrapperAddress, mControllerPort, mCertificateManager.generateSSLContext(), mIdentity);
        mMetricsManager = new MetricsManager(mAppContext);

        Handler mainHandler = new Handler(Looper.getMainLooper());
        new Thread(() -> {
            try {
                // Register
                // If automatic registration is disabled, we will assume that the UE has already been registered manually into the orc8r.
                // Bootstrapping will fail if the registered UE can't be found.
                boolean isRegistered = !mUseAutomaticRegistration || mRegistrationManager.registerSync();

                Certificate certificate = null;
                //Bootstrap
                if (isRegistered) {
                    certificate = mBootstrapManager.bootstrapSync();
                }

                //Store certificate & setup metrics manager
                if (certificate != null) {
                    mCertificateManager.addBootstrapCertificate(certificate);
                    mainHandler.post(this::onReady);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        }).start();
    }

    /**
     * Push a custom metric using Prometheus' {@link io.openschema.mma.metrics.Untyped Untyped} data. Will automatically use the registered
     * UUID & timestamp values. This method required the agent to have completed the bootstrapping process by calling {@link #init()}.
     *
     * <p>Example:
     * <pre>
     *     List<Pair<String, String>> metricValues = new ArrayList<>();
     *     metricValues.add(new Pair<>("lat", "25.761681"));
     *     metricValues.add(new Pair<>("long", "-80.191788"));
     *
     *     mma.pushUntypedMetric("location", metricValues);
     * </pre>
     *
     * <p>Generates:
     * <pre>
     *     "location" : {
     *         "lat" : "25.761681",
     *         "long" : "-80.191788"
     *     }
     * </pre>
     *
     * @param metricName   Root name for the group of collected metrics
     * @param metricValues List of metrics to collect with the <name, value> structure
     */
    public void pushUntypedMetric(String metricName, List<Pair<String, String>> metricValues) {
        mMetricsManager.collect(metricName, metricValues);
    }

    /**
     * Method called once the whole bootstrapping sequence started with {@link #init()} is completed.
     */
    private void onReady() {
        mIsReady = true;

        // Check if the static network and device metrics should be pushed
        if (mEnableLibraryMetrics) {
            attemptFirstTimeSetup();

            //TODO: remove or move to be periodically collected
            mMetricsManager.collect(CellularNetworkMetrics.METRIC_FAMILY_NAME, new CellularNetworkMetrics(mAppContext).retrieveNetworkMetrics());
            mMetricsManager.collect(WifiNetworkMetrics.METRIC_FAMILY_NAME, new WifiNetworkMetrics(mAppContext).retrieveNetworkMetrics());
//            UsageDataWorker.enqueuePeriodicWorker(mAppContext);
        }

        mMetricsManager.startWorker(mAppContext, mControllerAddress, mControllerPort, mMetricsAuthorityHeader);
    }

    //TODO: javadoc
    private void attemptFirstTimeSetup() {
        //Check if this is the first time the SDK runs
        SharedPreferences sharedPref = SharedPreferencesHelper.getInstance(mAppContext);
        boolean isFirstTime = sharedPref.getBoolean(SharedPreferencesHelper.KEY_FIRST_TIME_SETUP, true);

        if (isFirstTime) {
            //Run any code required only the first time the SDK is started
            executeFirstTimeSetup();

            //Save sharedpref to prevent this code from running any subsequent start
            sharedPref.edit()
                    .putBoolean(SharedPreferencesHelper.KEY_FIRST_TIME_SETUP, false)
                    .apply();
        }
    }

    //TODO: javadoc
    private void executeFirstTimeSetup(){
        mMetricsManager.collect(DeviceMetrics.METRIC_FAMILY_NAME, new DeviceMetrics(mAppContext).retrieveDeviceMetrics());
    }

    /**
     * Builder class for {@link MobileMetricsAgent} objects.
     *
     * <p>Example:
     *
     * <pre>
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
        private int mControllerCertificateResId;
        private String mMetricsAuthorityHeader;
        private int mControllerPort;
        private boolean mUseAutomaticRegistration = true;
        private String mBackendBaseURL;
        private int mBackendCertificateResId;
        private String mBackendUsername;
        private String mBackendPassword;
        private boolean mEnableLibraryMetrics = true;

        private Context mAppContext;


        /**
         * @param address URL of Magma's controller
         */
        public Builder setControllerAddress(String address) {
            mControllerAddress = address;
            return this;
        }

        /**
         * @param address URL of the bootstrapper's controller
         */
        public Builder setBootstrapperAddress(String address) {
            mBootstrapperAddress = address;
            return this;
        }

        /**
         * @param certificateResId Resource ID of the magma controller's raw certificate
         */
        public Builder setControllerCertificateResId(int certificateResId) {
            mControllerCertificateResId = certificateResId;
            return this;
        }

        /**
         * @param address URL of the metrics' controller
         */
        public Builder setAuthorityHeader(String address) {
            mMetricsAuthorityHeader = address;
            return this;
        }

        /**
         * @param port Port used by the bootstrapper's controller
         */
        public Builder setControllerPort(int port) {
            mControllerPort = port;
            return this;
        }

        /**
         * @param enabled Flag to enable or disable automatic registration using OpenSchema's backend
         */
        public Builder setUseAutomaticRegistration(boolean enabled) {
            mUseAutomaticRegistration = enabled;
            return this;
        }

        /**
         * @param baseURL Base URL of OpenSchema's middle box
         */
        public Builder setBackendBaseURL(String baseURL) {
            mBackendBaseURL = baseURL;
            return this;
        }

        /**
         * @param certificateResId Resource ID of the OpenSchema's middle box raw certificate
         */
        public Builder setBackendCertificateResId(int certificateResId) {
            mBackendCertificateResId = certificateResId;
            return this;
        }

        /**
         * @param backendUsername Secret username to access OpenSchema's middle box Basic Auth
         */
        public Builder setBackendUsername(String backendUsername) {
            mBackendUsername = backendUsername;
            return this;
        }

        /**
         * @param backendPassword Secret password to access OpenSchema's middle box Basic Auth
         */
        public Builder setBackendPassword(String backendPassword) {
            mBackendPassword = backendPassword;
            return this;
        }

        /**
         * @param enabled Boolean flag to determine if the static library metrics
         *                will be collected.
         */
        public Builder setEnabledLibraryMetrics(boolean enabled) {
            mEnableLibraryMetrics = enabled;
            return this;
        }

        /**
         * @param appContext Application context
         */
        public Builder setAppContext(Context appContext) {
            if (!(appContext instanceof Application)) {
                appContext = appContext.getApplicationContext();
            }
            mAppContext = appContext;
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
