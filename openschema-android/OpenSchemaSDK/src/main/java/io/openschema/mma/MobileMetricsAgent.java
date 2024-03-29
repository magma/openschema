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
import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.util.List;

import androidx.core.util.Pair;
import io.openschema.mma.backend.CertificateManager;
import io.openschema.mma.id.Identity;
import io.openschema.mma.metrics.MetricsManager;
import io.openschema.mma.utils.PersistentNotification;

/**
 * Main class to act as an interface to access the functionality in the library.
 */
public class MobileMetricsAgent {

    private static final String TAG = "MobileMetricsAgent";

    private final String mBackendBaseURL;
    private final int mBackendCertificateResId;
    private final String mBackendUsername;
    private final String mBackendPassword;
    private final boolean mEnableLibraryMetrics;

    private final Context mAppContext;
    private final Notification mCustomNotification;
    private Identity mIdentity;
    private CertificateManager mCertificateManager;
    private boolean mIsReady = false;

    private MetricsManager mMetricsManager = null;

    private MobileMetricsAgent(Builder mmaBuilder) {
        mBackendBaseURL = mmaBuilder.mBackendBaseURL;
        mBackendCertificateResId = mmaBuilder.mBackendCertificateResId;
        mBackendUsername = mmaBuilder.mBackendUsername;
        mBackendPassword = mmaBuilder.mBackendPassword;
        mEnableLibraryMetrics = mmaBuilder.mEnableLibraryMetrics;

        mAppContext = mmaBuilder.mAppContext;
        mCustomNotification = mmaBuilder.mCustomNotification;
    }

    /**
     * Initialize the object using the parameters supplied by the {@link Builder Builder}
     * <p>
     * {@link MobileMetricsAgent} needs to be
     * initialized before attempting to push information to the data lake.
     */
    public void init() {
        Log.d(TAG, "MMA: Initializing MMA...");

        //Initialize identity & certificates
        mIdentity = new Identity(mAppContext);
        mCertificateManager = new CertificateManager();
        mCertificateManager.addBackendCertificate(mAppContext, mBackendCertificateResId);

        //Initialize managers
        mMetricsManager = new MetricsManager(mAppContext);

        onReady();
    }

    /**
     * Push a custom metric. Will automatically use the registered UUID & timestamp values.
     *
     * <p>Example:
     * <pre>
     *     List<Pair<String, String>> metricValues = new ArrayList<>();
     *     metricValues.add(new Pair<>("lat", "25.761681"));
     *     metricValues.add(new Pair<>("long", "-80.191788"));
     *
     *     mma.pushMetric("location", metricValues);
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
    public void pushMetric(String metricName, List<Pair<String, String>> metricValues) {
        mMetricsManager.collect(metricName, metricValues);
    }

    /**
     * Method called once the initialization sequence started with {@link #init()} is completed.
     */
    private void onReady() {
        mIsReady = true;

        // Check if the library's baseline metrics are enabled
        if (mEnableLibraryMetrics) {

            //Set custom notification if it was set on the builder
            if (mCustomNotification != null) {
                PersistentNotification persistentNotification = PersistentNotification.getInstance(mAppContext);
                persistentNotification.setCustomNotification(mCustomNotification);
            }

            mAppContext.startForegroundService(new Intent(mAppContext, MobileMetricsService.class));
        }

        mMetricsManager.startWorker(mAppContext, mBackendBaseURL, mBackendUsername, mBackendPassword);
    }

    /**
     * Builder class for {@link MobileMetricsAgent} objects.
     *
     * <p>Example:
     *
     * <pre>
     *     MobileMetricsAgent mma = new MobileMetricsAgent.Builder()
     *             .setAppContext(getApplicationContext())
     *             .setBackendBaseURL(getString(R.string.backend_base_url))
     *             .setBackendCertificateResId(R.raw.server)
     *             .setBackendUsername(getString(R.string.backend_username))
     *             .setBackendPassword(getString(R.string.backend_password))
     *             .build();
     *     </pre>
     */
    public static class Builder {
        private String mBackendBaseURL;
        private int mBackendCertificateResId;
        private String mBackendUsername;
        private String mBackendPassword;
        private boolean mEnableLibraryMetrics = true;
        //TODO: add flag to disable storing metrics locally for UI

        private Context mAppContext;
        private Notification mCustomNotification = null;

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
         * @param notification Custom notification to be used for the SDK's persistent notification
         */
        public Builder setCustomNotification(Notification notification) {
            mCustomNotification = notification;
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
