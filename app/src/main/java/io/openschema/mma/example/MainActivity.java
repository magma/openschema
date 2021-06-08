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

package io.openschema.mma.example;

import android.os.Bundle;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.List;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.util.Pair;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.NavigationUI;
import io.openschema.mma.MobileMetricsAgent;

public class MainActivity extends AppCompatActivity {

    private MobileMetricsAgent mMobileMetricsAgent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Setup bottom navigation
        NavController navController = Navigation.findNavController(this, R.id.main_content);
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation_bar);
        NavigationUI.setupWithNavController(bottomNavigationView, navController);

        //Build OpenSchema agent with required data
        mMobileMetricsAgent = new MobileMetricsAgent.Builder()
                .setAppContext(getApplicationContext())
                .setControllerAddress(getString(R.string.controller_address))
                .setControllerPort(getResources().getInteger(R.integer.controller_port))
                .setBootstrapperAddress(getString(R.string.bootstrapper_address))
                .setControllerCertificateResId(R.raw.controller)
                .setAuthorityHeader(getString(R.string.metrics_authority_header))
//                .setUseAutomaticRegistration(false)
                .setBackendBaseURL(getString(R.string.backend_base_url))
                .setBackendCertificateResId(R.raw.backend)
                .setBackendUsername(getString(R.string.backend_username))
                .setBackendPassword(getString(R.string.backend_password))
                .build();

        //Initialize agent
        try {
            mMobileMetricsAgent.init();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void pushMetric(String metricName, List<Pair<String, String>> metricValues) {
        mMobileMetricsAgent.pushUntypedMetric(metricName, metricValues);
    }
}