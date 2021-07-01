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

package io.openschema.client.viewmodel;

import android.app.Application;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import io.openschema.mma.data.MetricsRepository;
import io.openschema.mma.data.entity.NetworkUsageEntity;
import io.openschema.client.view.TimeSelector;

public class UsageViewModel extends AndroidViewModel {

    private static final String TAG = "UsageViewModel";
    private final MetricsRepository mMetricsRepository;

    private final MutableLiveData<TimeSelector.TimeWindow> mCurrentWindow = new MutableLiveData<>(TimeSelector.TimeWindow.DAY);
    private final LiveData<List<NetworkUsageEntity>> mCurrentWindowEntities;

    public UsageViewModel(@NonNull Application application) {
        super(application);
        mMetricsRepository = MetricsRepository.getRepository(application.getApplicationContext());
        mCurrentWindowEntities = Transformations.switchMap(mCurrentWindow, currentWindow -> {
            currentWindow.calculateWindow();

            //Debugging difference between info collected by the service & the usage tracked by the OS.
//            UsageRetriever usageRetriever = new UsageRetriever(getApplication().getApplicationContext());
//            long cellularTonnageOS = usageRetriever.getDeviceTonnage(NetworkCapabilities.TRANSPORT_CELLULAR, windowStart, windowEnd);
//            long wifiTonnageOS = usageRetriever.getDeviceTonnage(NetworkCapabilities.TRANSPORT_WIFI, windowStart, windowEnd);
//            Log.e(TAG, "UI: Tonnage difference between logged vs OS:" +
//                    "\nWindow start: " + FormattingUtils.humanReadableDate(windowStart) + ", Window end: " + FormattingUtils.humanReadableDate(windowEnd) +
//                    "\nCellular (OS): " + FormattingUtils.humanReadableByteCountSI(cellularTonnageOS) +
//                    "\nWi-Fi (OS): " + FormattingUtils.humanReadableByteCountSI(wifiTonnageOS));

            return mMetricsRepository.getUsageEntities(currentWindow.getWindowStart(), currentWindow.getWindowEnd());
        });
    }

    public void setCurrentTimeWindow(TimeSelector.TimeWindow newWindow) {
        mCurrentWindow.setValue(newWindow);
    }

    public LiveData<List<NetworkUsageEntity>> getUsageEntities() { return mCurrentWindowEntities; }
}
