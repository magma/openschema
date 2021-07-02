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

package io.openschema.client.fragment;

import android.app.AppOpsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import io.openschema.client.R;
import io.openschema.client.activity.OnboardingActivity;
import io.openschema.client.databinding.FragmentUsagePermissionBinding;
import io.openschema.client.util.PermissionManager;

public class UsagePermissionFragment extends Fragment {

    private static final String TAG = "UsagePermissionFragment";
    private FragmentUsagePermissionBinding mBinding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        mBinding = FragmentUsagePermissionBinding.inflate(inflater, container, false);
        return mBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        mBinding.usageContinueBtn.setOnClickListener(v -> requestPermission());

        ApplicationInfo appInfo = requireContext().getApplicationInfo();
        int appLabelRes = appInfo.labelRes;
        String appLabel = appLabelRes == 0 ? appInfo.nonLocalizedLabel.toString() : requireContext().getString(appLabelRes);
        mBinding.usageDescription.setText(String.format(getString(R.string.usage_permission_txt), appLabel));
    }

    private void continueToNextPage() {
        ((OnboardingActivity) requireActivity()).loadNextPage();
    }

    private void requestPermission() {
        final AppOpsManager appOps = (AppOpsManager) requireContext().getSystemService(Context.APP_OPS_SERVICE);
        appOps.startWatchingMode(AppOpsManager.OPSTR_GET_USAGE_STATS, requireContext().getPackageName(), new AppOpsManager.OnOpChangedListener() {
            @Override
            public void onOpChanged(String s, String s1) {
                boolean enabled = (AppOpsManager.MODE_ALLOWED == appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, requireContext().getApplicationInfo().uid, requireContext().getPackageName()));
                if (enabled) {
                    appOps.stopWatchingMode(this);
                    Intent intent = new Intent(requireContext(), OnboardingActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                }
            }
        });

        Intent i = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
        i.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        startActivity(i);
    }

    @Override
    public void onResume() {
        super.onResume();

        //Check if permission was granted on the app's settings
        if (PermissionManager.isUsagePermissionGranted(requireContext())) {
            continueToNextPage();
        }
    }
}