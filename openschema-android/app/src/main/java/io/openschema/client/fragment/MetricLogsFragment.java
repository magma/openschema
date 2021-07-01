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

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import io.openschema.mma.data.MetricsRepository;
import io.openschema.mma.data.entity.MetricsEntity;
import io.openschema.client.databinding.FragmentMetricLogsBinding;
import io.openschema.client.databinding.ViewMetricLogListEntryBinding;

public class MetricLogsFragment extends Fragment {

    private static final String TAG = "MetricLogsFragment";
    private FragmentMetricLogsBinding mBinding;

    private MetricsRepository mMetricsRepository;

    private MetricLogsListAdapter mRVAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        mBinding = FragmentMetricLogsBinding.inflate(inflater, container, false);
        mMetricsRepository = MetricsRepository.getRepository(requireContext());
        return mBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {

        mBinding.metricLogsList.setLayoutManager(new LinearLayoutManager(requireContext()));
        mRVAdapter = new MetricLogsListAdapter();
        mBinding.metricLogsList.setAdapter(mRVAdapter);

        mMetricsRepository.getEnqueuedMetrics().observe(getViewLifecycleOwner(), metricsEntities -> {
            if (metricsEntities != null) {
                mBinding.setIsDataAvailable(metricsEntities.size() != 0);
                mRVAdapter.submitList(metricsEntities);
            } else {
                mBinding.setIsDataAvailable(false);
            }
        });
    }

    private static class MetricLogsListAdapter extends ListAdapter<MetricsEntity, MetricLogsListAdapter.ViewHolder> {

        public MetricLogsListAdapter() {
            super(DIFF_CALLBACK);
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(ViewMetricLogListEntryBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
        }
        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            holder.bind(getItem(position));
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            private final ViewMetricLogListEntryBinding mBinding;

            public ViewHolder(ViewMetricLogListEntryBinding binding) {
                super(binding.getRoot());
                mBinding = binding;
            }

            public void bind(MetricsEntity metricsEntity) {
                mBinding.setEntryData(metricsEntity);
            }
        }

        private static final DiffUtil.ItemCallback<MetricsEntity> DIFF_CALLBACK = new DiffUtil.ItemCallback<MetricsEntity>() {
            @Override
            public boolean areItemsTheSame(@NonNull MetricsEntity oldItem, @NonNull MetricsEntity newItem) {
                return oldItem.getId() == newItem.getId();
            }
            @Override
            public boolean areContentsTheSame(@NonNull MetricsEntity oldItem, @NonNull MetricsEntity newItem) {
                //We don't need to implement an equals() comparison for now since the contents won't be changing
                return oldItem.getId() == newItem.getId();
            }
        };
    }
}