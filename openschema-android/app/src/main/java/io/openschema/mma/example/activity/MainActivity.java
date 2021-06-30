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

package io.openschema.mma.example.activity;

import android.app.Notification;
import android.app.PendingIntent;
import android.os.Bundle;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.List;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.NotificationCompat;
import androidx.core.util.Pair;
import androidx.navigation.NavController;
import androidx.navigation.NavDeepLinkBuilder;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import io.openschema.mma.MobileMetricsAgent;
import io.openschema.mma.example.R;
import io.openschema.mma.example.util.PermissionManager;
import io.openschema.mma.utils.PersistentNotification;

public class MainActivity extends AppCompatActivity {

    private MobileMetricsAgent mMobileMetricsAgent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (checkMandatoryPermissions()) {
            NavController navController = Navigation.findNavController(this, R.id.main_content);

            //Setup bottom navigation
            BottomNavigationView bottomNavigationView = findViewById(R.id.main_bottom_navigation_bar);
            NavigationUI.setupWithNavController(bottomNavigationView, navController);

            //Setup tool bar (top app bar)
            AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(R.id.nav_usage, R.id.nav_map, R.id.nav_metric_logs).build();
            Toolbar toolbar = findViewById(R.id.main_toolbar);
            NavigationUI.setupWithNavController(toolbar, navController, appBarConfiguration);

            //Build OpenSchema agent with required data
            mMobileMetricsAgent = new MobileMetricsAgent.Builder()
                    .setAppContext(getApplicationContext())
                    .setCustomNotification(createCustomNotification())
                    .setBackendBaseURL(getString(R.string.backend_base_url))
                    .setBackendCertificateResId(R.raw.backend)
                    .setBackendUsername(getString(R.string.backend_username))
                    .setBackendPassword(getString(R.string.backend_password))
                    .build();

            //Initialize agent
            mMobileMetricsAgent.init();
        }
    }

    public void pushMetric(String metricName, List<Pair<String, String>> metricValues) {
        mMobileMetricsAgent.pushMetric(metricName, metricValues);
    }

    private boolean checkMandatoryPermissions() {
        if (!PermissionManager.areMandatoryPermissionsGranted(this)) {
            //Redirect to onboarding to go through the required permissions
            NavController navController = Navigation.findNavController(this, R.id.main_content);
            navController.navigate(R.id.action_to_onboarding);
            finish();
            return false;
        }
        return true;
    }

    private Notification createCustomNotification() {
        //Create intent to open main page
        Bundle args = new Bundle();
        args.putInt("requestCode", PersistentNotification.SERVICE_NOTIFICATION_ID);
        PendingIntent pendingIntent = new NavDeepLinkBuilder(this)
                .setComponentName(MainActivity.class)
                .setGraph(R.navigation.nav_graph_main)
                .setDestination(R.id.nav_usage)
                .setArguments(args)
                .createPendingIntent();

        return new NotificationCompat.Builder(this, PersistentNotification.SERVICE_NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(io.openschema.mma.R.drawable.persistent_notification_icon)
                .setContentTitle("OpenSchema is running")
                .setContentText("Tap here for more information.")
                .setOngoing(true) //notification can't be swiped
                .setShowWhen(false)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setContentIntent(pendingIntent)
                .build();
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkMandatoryPermissions();
    }
}