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

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.Navigation;
import io.openschema.mma.example.R;
import io.openschema.mma.example.util.PermissionManager;

public class OnboardingActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);
        loadNextPage();
    }

    private void loadNextPage() {
        //TODO: Need a ToS page to disclose data collection?
        //TODO: Add battery optimization whitelisting as requirement?

        int targetDestination = -1;

        if (!PermissionManager.isLocationPermissionGranted(this)) {
            targetDestination = R.id.action_to_location_permission;
        }

        if (!PermissionManager.isPhonePermissionGranted(this)) {
            targetDestination = R.id.action_to_phone_permission;
        }

        if (!PermissionManager.isUsagePermissionGranted(this)) {
            targetDestination = R.id.action_to_usage_permission;
        }

        if (targetDestination != -1) {
            Navigation.findNavController(this, R.id.onboarding_content)
                    .navigate(targetDestination);
        } else {
            continueToMain();
        }
    }

    public void continueToMain() {
        Navigation.findNavController(this, R.id.onboarding_content)
                .navigate(R.id.action_to_main);
        finish();
    }
}