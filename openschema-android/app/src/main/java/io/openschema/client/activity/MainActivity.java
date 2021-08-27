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

package io.openschema.client.activity;

import android.os.Bundle;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;

import java.util.List;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.util.Pair;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import io.openschema.client.R;
import io.openschema.client.util.PermissionManager;
import io.openschema.client.view.CustomNotification;
import io.openschema.client.view.NetworkQualityView;
import io.openschema.client.viewmodel.NetworkQualityViewModel;
import io.openschema.mma.MobileMetricsAgent;
import io.openschema.mma.MobileMetricsService;

public class MainActivity extends AppCompatActivity {

    private MobileMetricsAgent mMobileMetricsAgent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (checkMandatoryPermissions()) {
            NavController navController = Navigation.findNavController(this, R.id.main_content);

            //Setup navigation drawer
            DrawerLayout navigationDrawer = findViewById(R.id.main_nav_drawer_layout);
            NavigationView navigationView = findViewById(R.id.main_nav_drawer);
            NavigationUI.setupWithNavController(navigationView, navController);

            //Setup bottom navigation
            BottomNavigationView bottomNavigationView = findViewById(R.id.main_bottom_navigation_bar);
            NavigationUI.setupWithNavController(bottomNavigationView, navController);

            //Setup tool bar (top app bar)
            AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(R.id.nav_usage, R.id.nav_map, R.id.nav_metric_logs)
                    .setOpenableLayout(navigationDrawer)
                    .build();
            Toolbar toolbar = findViewById(R.id.main_toolbar);
            NavigationUI.setupWithNavController(toolbar, navController, appBarConfiguration);

            //Build OpenSchema agent with required data
            mMobileMetricsAgent = new MobileMetricsAgent.Builder()
                    .setAppContext(getApplicationContext())
                    .setCustomNotification(CustomNotification.getInstance(getApplicationContext()).getNotification())
                    .setBackendBaseURL(getString(R.string.backend_base_url))
                    .setBackendCertificateResId(R.raw.backend)
                    .setBackendUsername(getString(R.string.backend_username))
                    .setBackendPassword(getString(R.string.backend_password))
                    .build();

            //Start permanent observer to update custom notification. (Only if service isn't running so that we don't create multiple observers on subsequent app starts)
            if (!MobileMetricsService.isServiceRunning(getApplicationContext())) {
                NetworkQualityViewModel networkQualityViewModel = new ViewModelProvider(this).get(NetworkQualityViewModel.class);
                networkQualityViewModel.getActiveNetworkQuality().observeForever(networkQualityEntity -> {
                    CustomNotification customNotification = CustomNotification.getInstance(getApplicationContext());

                    //Update the notification's view
                    if (networkQualityEntity == null) {
                        customNotification.updateNetworkStatus(null);
                    } else {
                        customNotification.updateNetworkStatus(new NetworkQualityView.NetworkStatus(networkQualityEntity.getTransportType(), networkQualityEntity.getQualityScore()));
                    }

                    //Update the notification shown by the OS
                    customNotification.show(getApplicationContext());
                });
            }

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

    @Override
    protected void onResume() {
        super.onResume();
        checkMandatoryPermissions();
    }
}