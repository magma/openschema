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

import java.util.ArrayList;
import java.util.List;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.util.Pair;
import io.openschema.mma.MobileMetricsAgent;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Build agent with required data
        MobileMetricsAgent mma = new MobileMetricsAgent.Builder()
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
            mma.init();
        } catch (Exception e) {
            e.printStackTrace();
        }

        //Queue metrics
//        List<Pair<String, String>> metricValues = new ArrayList<>();
//        metricValues.add(new Pair<>("customValue1", "value1"));
//        metricValues.add(new Pair<>("customValue2", "2"));
//        mma.pushUntypedMetric("customMetric_example1", metricValues);

//        List<Pair<String, String>> metricValues2 = new ArrayList<>();
//        metricValues2.add(new Pair<>("customValue3", "value3"));
//        metricValues2.add(new Pair<>("customValue4", "4"));
//        mma.pushUntypedMetric("customMetric_example2", metricValues2);
    }
}