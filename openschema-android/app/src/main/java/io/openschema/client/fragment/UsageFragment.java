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

import android.net.NetworkCapabilities;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.mackhartley.roundedprogressbar.RoundedProgressBar;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.BindingAdapter;
import androidx.databinding.BindingMethod;
import androidx.databinding.BindingMethods;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import io.openschema.client.databinding.FragmentUsageBinding;
import io.openschema.client.util.FormattingUtils;
import io.openschema.client.view.NetworkQualityView;
import io.openschema.client.viewmodel.NetworkQualityViewModel;
import io.openschema.client.viewmodel.UsageViewModel;
import io.openschema.mma.data.entity.HourlyUsageEntity;
import io.openschema.mma.data.entity.NetworkQualityEntity;


public class UsageFragment extends Fragment {

    private static final String TAG = "UsageFragment";
    private FragmentUsageBinding mBinding;
    private UsageViewModel mUsageViewModel;
    private NetworkQualityViewModel mNetworkQualityViewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        mBinding = FragmentUsageBinding.inflate(inflater, container, false);
        mUsageViewModel = new ViewModelProvider(this).get(UsageViewModel.class);
        mNetworkQualityViewModel = new ViewModelProvider(requireActivity()).get(NetworkQualityViewModel.class); //Sharing MainActivity's ViewModel instance
        return mBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mUsageViewModel.getHourlyUsageEntities().observe(getViewLifecycleOwner(), this::updateHourlyTonnageChart);

        mNetworkQualityViewModel.getActiveNetworkQuality().observe(getViewLifecycleOwner(), this::updateNetworkQualityView);

        mBinding.usageTimeSelector.setOnTimeWindowChangedListener(newWindow -> {
            mUsageViewModel.setCurrentTimeWindow(newWindow);
        });

        mBinding.usageNetworkQuality.setMeasureBtnClickListener(view1 -> {
            mNetworkQualityViewModel.remeasureNetworkQuality();
        });
    }

    private void updateHourlyTonnageChart(List<HourlyUsageEntity> hourlyUsageEntities) {
        //TODO: optimize to prevent recalculating entries we've already seen when new entries are received.
        // Will only be able to optimize while the current window is still the same. Not urgent since most people wouldn't stay constantly looking at this page.
        long cellularTonnage = 0, wifiTonnage = 0;

        if (hourlyUsageEntities != null) {
            Log.d(TAG, "UI: " + hourlyUsageEntities.size() + " entities are being used for this calculation.");
            //Calculate tonnage for each network type
            for (int i = 0; i < hourlyUsageEntities.size(); i++) {
                HourlyUsageEntity currentEntity = hourlyUsageEntities.get(i);
                switch (currentEntity.getTransportType()) {
                    case NetworkCapabilities.TRANSPORT_CELLULAR:
                        cellularTonnage += currentEntity.getUsage();
                        break;
                    case NetworkCapabilities.TRANSPORT_WIFI:
                        wifiTonnage += currentEntity.getUsage();
                        break;
                }
            }
        }

        //Set the data to the layout
        mBinding.setHourlyData(new UsageData(cellularTonnage, wifiTonnage));
    }

    private void updateNetworkQualityView(NetworkQualityView.NetworkStatus networkStatus) {
        mBinding.usageNetworkQuality.setNetworkData(networkStatus);
    }

    /**
     * POJO to hold the usage information and feed the data to the layout through data binding.
     */
    @BindingMethods({
                            @BindingMethod(type = com.mackhartley.roundedprogressbar.RoundedProgressBar.class,
                                           attribute = "rpbProgress",
                                           method = "setRpbProgress"),
                    })
    public static class UsageData {
        private final long mCellularTonnage;
        private final long mWifiTonnage;
        private final long mTotalTonnage;

        public UsageData(long cellularTonnage, long wifiTonnage) {
            mCellularTonnage = cellularTonnage;
            mWifiTonnage = wifiTonnage;
            mTotalTonnage = cellularTonnage + wifiTonnage;
        }

        public double getCellularPercentage() {
            return mTotalTonnage == 0 ? 0 : (100 * (double) mCellularTonnage / mTotalTonnage);
        }

        public double getWifiPercentage() {
            return mTotalTonnage == 0 ? 0 : (100 * (double) mWifiTonnage / mTotalTonnage);
        }

        public String getCellularValue() {
            return FormattingUtils.humanReadableByteCountSI(mCellularTonnage).replaceAll(" ", "\n");
        }

        public String getWifiValue() {
            return FormattingUtils.humanReadableByteCountSI(mWifiTonnage).replaceAll(" ", "\n");
        }

        /**
         * Binding adapter to match app:rpbProgress to setProgressPercentage() when using data binding in the layout.
         */
        @BindingAdapter("rpbProgress")
        public static void setRpbProgress(RoundedProgressBar v, double progressPercentage) {
            v.setProgressPercentage(progressPercentage, true);
        }
    }
}