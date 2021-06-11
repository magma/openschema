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

package io.openschema.mma.example.fragment;

import android.Manifest;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import io.openschema.mma.example.R;
import io.openschema.mma.example.databinding.FragmentPhonePermissionBinding;
import io.openschema.mma.example.util.PermissionManager;

public class PhonePermissionFragment extends Fragment {

    private static final String TAG = "PhonePermissionFragment";

    private FragmentPhonePermissionBinding mBinding;

    private final ActivityResultLauncher<String> mPhonePermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
        if (isGranted) {
            continueToNextPage();
        } else {
            if (!shouldShowRequestPermissionRationale(Manifest.permission.READ_PHONE_STATE)) {
                //User selected the "Don't ask again" option when denying the permission
                openAppDetails();
            }
        }
    });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        mBinding = FragmentPhonePermissionBinding.inflate(inflater, container, false);
        return mBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        mBinding.phoneContinueBtn.setOnClickListener(v -> requestPermission());
    }

    private void continueToNextPage() {
        NavHostFragment.findNavController(this)
                .navigate(R.id.action_to_location_permission);
    }

    private void requestPermission() {
        mPhonePermissionLauncher.launch(Manifest.permission.READ_PHONE_STATE);
    }

    private void openAppDetails() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.fromParts("package", requireContext().getPackageName(), null));
        startActivity(intent);
    }

    @Override
    public void onResume() {
        super.onResume();

        //Check if permission was granted on the app's settings
        if (PermissionManager.isPhonePermissionGranted(requireContext())) {
            continueToNextPage();
        }
    }
}