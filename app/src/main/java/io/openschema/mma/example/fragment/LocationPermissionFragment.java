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

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import io.openschema.mma.example.R;
import io.openschema.mma.example.activity.OnboardingActivity;
import io.openschema.mma.example.databinding.FragmentLocationPermissionBinding;
import io.openschema.mma.example.databinding.FragmentMainBinding;

public class LocationPermissionFragment extends Fragment {

    private static final String TAG = "LocationPermissionFragment";
    private FragmentLocationPermissionBinding mBinding;

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
        mBinding.locationContinueBtn.setOnClickListener(v -> {
            ((OnboardingActivity) requireActivity()).continueToMain();
        });
    }
}