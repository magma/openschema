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

import android.net.NetworkCapabilities;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;

import java.util.Arrays;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import io.openschema.mma.data.entity.NetworkUsageEntity;
import io.openschema.mma.example.R;
import io.openschema.mma.example.databinding.FragmentUsageBinding;
import io.openschema.mma.example.util.FormattingUtils;
import io.openschema.mma.example.viewmodel.UsageViewModel;

public class UsageFragment extends Fragment {

    private static final String TAG = "UsageFragment";
    private FragmentUsageBinding mBinding;
    private UsageViewModel mViewModel;

    private PieDataSet mDataSet;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        mBinding = FragmentUsageBinding.inflate(inflater, container, false);
        mViewModel = new ViewModelProvider(this).get(UsageViewModel.class);
        return mBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initTonnageChart();

        mViewModel.getUsageEntities().observe(getViewLifecycleOwner(), networkUsageEntities -> {
            //TODO: optimize to prevent recalculating entries we've already seen when new entries are received.
            // Will only be able to optimize while the current window is still the same. Not urgent since most people wouldn't stay constantly looking at this page.
            long cellularTonnage = 0, wifiTonnage = 0;

            if (networkUsageEntities != null) {
                Log.d(TAG, "UI: " + networkUsageEntities.size() + " entities are being used for this calculation.");
                //Calculate tonnage for each network type
                for (int i = 0; i < networkUsageEntities.size(); i++) {
                    NetworkUsageEntity currentEntity = networkUsageEntities.get(i);
                    switch (currentEntity.mTransportType) {
                        case NetworkCapabilities.TRANSPORT_CELLULAR:
                            cellularTonnage += currentEntity.mUsage;
                            break;
                        case NetworkCapabilities.TRANSPORT_WIFI:
                            wifiTonnage += currentEntity.mUsage;
                            break;
                    }
                }

                //Debugging difference between info collected by the service & the usage tracked by the OS.
//                Log.e(TAG, "UI: Tonnage difference between logged vs OS:" +
//                        "\nCellular (Counted): " + FormattingUtils.humanReadableByteCountSI(cellularTonnage) +
//                        "\nWi-Fi (Counted): " + FormattingUtils.humanReadableByteCountSI(wifiTonnage));

            }

            //Set data available flag to hide chart if no data is available.
            mBinding.setIsDataAvailable(cellularTonnage != 0 || wifiTonnage != 0);

            //Set calculated tonnage to pie chart
            mDataSet.getEntryForIndex(NetworkCapabilities.TRANSPORT_CELLULAR).setY(cellularTonnage);
            mDataSet.getEntryForIndex(NetworkCapabilities.TRANSPORT_WIFI).setY(wifiTonnage);
            mBinding.usageTonnageChart.setCenterText(FormattingUtils.humanReadableByteCountSI(cellularTonnage + wifiTonnage));

            //Apply data changes
            mBinding.usageTonnageChart.getData().notifyDataChanged();
            mBinding.usageTonnageChart.notifyDataSetChanged();
            mBinding.usageTonnageChart.invalidate();
        });

        mBinding.usageWindowLeft.setOnClickListener(v -> mViewModel.moveUsageWindow(-1));
        mBinding.usageWindowRight.setOnClickListener(v -> mViewModel.moveUsageWindow(1));

        mViewModel.getCurrentWindowTxt().observe(getViewLifecycleOwner(), s -> mBinding.setCurrentWindowTxt(s));
    }

    private void initTonnageChart() {
        //Init data
        PieEntry[] newEntries = new PieEntry[2];
        newEntries[NetworkCapabilities.TRANSPORT_CELLULAR] = new PieEntry(0, "Cellular");
        newEntries[NetworkCapabilities.TRANSPORT_WIFI] = new PieEntry(0, "Wi-Fi");

        mDataSet = new PieDataSet(Arrays.asList(newEntries), "");
        mDataSet.setColors(ContextCompat.getColor(requireContext(), R.color.wifiColor), ContextCompat.getColor(requireContext(), R.color.cellularColor));
        mDataSet.setSliceSpace(5);
        mDataSet.setDrawValues(false);

        //Configure chart
        mBinding.usageTonnageChart.setData(new PieData(mDataSet));
        mBinding.usageTonnageChart.setCenterTextSize(28);
        mBinding.usageTonnageChart.setHoleRadius(60);
        mBinding.usageTonnageChart.setTransparentCircleAlpha(0);
        mBinding.usageTonnageChart.setTouchEnabled(false);
        mBinding.usageTonnageChart.getDescription().setText("");
        mBinding.usageTonnageChart.getLegend().setEnabled(false);
    }
}