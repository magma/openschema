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

package io.openschema.mma.example.viewmodel;

import android.app.Application;
import android.util.Log;

import java.util.Calendar;
import java.util.List;
import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import io.openschema.mma.data.MetricsRepository;
import io.openschema.mma.data.entity.NetworkUsageEntity;
import io.openschema.mma.utils.CalendarUtils;

public class UsageViewModel extends AndroidViewModel {

    private static final String TAG = "UsageViewModel";
    private final MetricsRepository mMetricsRepository;

    private final MutableLiveData<UsageWindow> mCurrentWindow = new MutableLiveData<>(UsageWindow.DAY);
    private final LiveData<String> mCurrentWindowTxt = Transformations.map(mCurrentWindow, UsageWindow::toString);
    private final LiveData<List<NetworkUsageEntity>> mCurrentWindowEntities;

    public UsageViewModel(@NonNull Application application) {
        super(application);
        mMetricsRepository = MetricsRepository.getRepository(application.getApplicationContext());
        mCurrentWindowEntities = Transformations.switchMap(mCurrentWindow, currentWindow -> {
            final long windowStart;
            final long windowEnd;
            switch (currentWindow) {
                case HOUR:
                    Calendar hourCal = CalendarUtils.getCurrentHourCalendar();
                    windowStart = hourCal.getTimeInMillis();
                    windowEnd = windowStart + TimeUnit.HOURS.toMillis(1);
                    break;
                case DAY:
                    Calendar dayCal = CalendarUtils.getCurrentDayCalendar();
                    windowStart = dayCal.getTimeInMillis();
                    windowEnd = windowStart + TimeUnit.DAYS.toMillis(1);
                    break;
                default: //Month
                    Calendar monthCal = CalendarUtils.getCurrentMonthCalendar();
                    Calendar nextMonthCal = Calendar.getInstance();
                    nextMonthCal.setTimeInMillis(monthCal.getTimeInMillis());
                    nextMonthCal.add(Calendar.MONTH, 1);
                    windowStart = monthCal.getTimeInMillis();
                    windowEnd = nextMonthCal.getTimeInMillis();
                    break;
            }

            //Debugging difference between info collected by the service & the usage tracked by the OS.
//            UsageRetriever usageRetriever = new UsageRetriever(getApplication().getApplicationContext());
//            long cellularTonnageOS = usageRetriever.getDeviceTonnage(NetworkCapabilities.TRANSPORT_CELLULAR, windowStart, windowEnd);
//            long wifiTonnageOS = usageRetriever.getDeviceTonnage(NetworkCapabilities.TRANSPORT_WIFI, windowStart, windowEnd);
//            Log.e(TAG, "UI: Tonnage difference between logged vs OS:" +
//                    "\nWindow start: " + FormattingUtils.humanReadableDate(windowStart) + ", Window end: " + FormattingUtils.humanReadableDate(windowEnd) +
//                    "\nCellular (OS): " + FormattingUtils.humanReadableByteCountSI(cellularTonnageOS) +
//                    "\nWi-Fi (OS): " + FormattingUtils.humanReadableByteCountSI(wifiTonnageOS));

            return mMetricsRepository.getUsageEntities(windowStart, windowEnd);
        });
    }

    public void moveUsageWindow(int delta) {
        if (delta == 0) return;

        UsageWindow[] enumValues = UsageWindow.values();
        int currentValue = mCurrentWindow.getValue().ordinal();
        currentValue += delta;

        if (currentValue < 0 || currentValue >= enumValues.length) return;

        UsageWindow newValue = enumValues[currentValue];
        Log.d(TAG, "UI: Usage window has changed to: " + newValue.name());
        mCurrentWindow.setValue(newValue);
    }

    public LiveData<String> getCurrentWindowTxt() { return mCurrentWindowTxt;}

    public LiveData<List<NetworkUsageEntity>> getUsageEntities() { return mCurrentWindowEntities; }

    private enum UsageWindow {
        HOUR,
        DAY,
        MONTH;

        @NonNull
        @Override
        public String toString() {
            return this.name().substring(0, 1) + this.name().substring(1).toLowerCase();
        }
    }
}
