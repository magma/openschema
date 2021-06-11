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
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import io.openschema.mma.example.activity.OnboardingActivity;
import io.openschema.mma.example.databinding.FragmentLocationPermissionBinding;
import io.openschema.mma.example.util.PermissionManager;

public class LocationPermissionFragment extends Fragment {

    private static final String TAG = "LocationPermissionFragment";
    private FragmentLocationPermissionBinding mBinding;

    private final ActivityResultLauncher<String> mLocationPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
        if (isGranted) {
            //Additional background location permission is required for Android 10+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                requestPermission();
            } else {
                continueToNextPage();
            }
        } else {
            if (!shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
                //User selected the "Don't ask again" option when denying the permission
                openAppDetails();
            }
        }
    });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        mBinding = FragmentLocationPermissionBinding.inflate(inflater, container, false);
        return mBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        //TODO: Only enable continue button after permission is accepted
        mBinding.locationContinueBtn.setOnClickListener(v -> requestPermission());
    }

    private void continueToNextPage() {
        ((OnboardingActivity) requireActivity()).continueToMain();
    }

    private void requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (!PermissionManager.isLocationPermissionGranted(requireContext())) {
                mLocationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
            } else {
                //Using a custom dialog to explain better the need for background location. Using registerForActivityResult() for ACCESS_BACKGROUND_LOCATION has a different behavior.
                if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_BACKGROUND_LOCATION)) {
                    //TODO: Listen to user changing the location settings to "all the time" to automatically return to the app, similar to usage permission behavior.
                    new AlertDialog.Builder(requireContext())
                            .setTitle("Background Location Permission")
                            .setMessage("This application requires accessing location from the background. Please set the permission to \"Allow all the time\".")
                            .setPositiveButton("Allow in Settings", (dialog, which) -> {
                                requestPermissions(new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION}, 0);
                            })
                            .setNegativeButton("Deny", (dialog, which) -> {
                                dialog.cancel();
                            })
                            .create()
                            .show();
                } else {
                    openAppDetails();
                }
            }
        } else {
            mLocationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
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
        if (PermissionManager.isLocationPermissionGranted(requireContext())) {
            //Additional background location permission is required for Android 10+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                if (PermissionManager.isBackgroundLocationPermissionGranted(requireContext())) {
                    continueToNextPage();
                }
            } else {
                continueToNextPage();
            }
        }
    }
}