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
import android.content.SharedPreferences;
import android.net.NetworkCapabilities;
import android.os.Handler;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import io.openschema.client.view.TimeSelector;
import io.openschema.mma.data.MetricsRepository;
import io.openschema.mma.data.entity.HourlyUsageEntity;
import io.openschema.mma.data.entity.NetworkUsageEntity;
import io.openschema.mma.utils.SharedPreferencesHelper;
import io.openschema.mma.utils.UsageRetriever;

public class UsageViewModel extends AndroidViewModel {

    private static final String TAG = "UsageViewModel";
    private final MetricsRepository mMetricsRepository;

    private final MutableLiveData<TimeSelector.TimeWindow> mCurrentWindow = new MutableLiveData<>(TimeSelector.TimeWindow.DAY);
    private final LiveData<List<NetworkUsageEntity>> mCurrentWindowEntities;
    private final HourlyLiveData mCurrentWindowHourlyEntities;

    public UsageViewModel(@NonNull Application application) {
        super(application);
        mMetricsRepository = MetricsRepository.getRepository(application.getApplicationContext());
        mCurrentWindowEntities = Transformations.switchMap(mCurrentWindow, currentWindow -> {
            currentWindow.calculateWindow();

//            //Debugging difference between info collected by the service & the usage tracked by the OS.
//            UsageRetriever usageRetriever = new UsageRetriever(getApplication().getApplicationContext());
//            long cellularTonnageOS = usageRetriever.getDeviceTonnage(NetworkCapabilities.TRANSPORT_CELLULAR, currentWindow.getWindowStart(), currentWindow.getWindowEnd());
//            long wifiTonnageOS = usageRetriever.getDeviceTonnage(NetworkCapabilities.TRANSPORT_WIFI, currentWindow.getWindowStart(), currentWindow.getWindowEnd());
//            Log.e(TAG, "UI: Tonnage difference between logged vs OS:" +
//                    "\nWindow start: " + FormattingUtils.humanReadableDate(currentWindow.getWindowStart()) + ", Window end: " + FormattingUtils.humanReadableDate(currentWindow.getWindowEnd()) +
//                    "\nCellular (OS): " + FormattingUtils.humanReadableByteCountSI(cellularTonnageOS) +
//                    "\nWi-Fi (OS): " + FormattingUtils.humanReadableByteCountSI(wifiTonnageOS));

            return mMetricsRepository.getUsageEntities(currentWindow.getWindowStart(), currentWindow.getWindowEnd());
        });

        mCurrentWindowHourlyEntities = new HourlyLiveData(Transformations.switchMap(mCurrentWindow, currentWindow -> {
            currentWindow.calculateWindow();
            return mMetricsRepository.getHourlyUsageEntities(currentWindow.getWindowStart(), currentWindow.getWindowEnd());
        }), application);
    }

    public void setCurrentTimeWindow(TimeSelector.TimeWindow newWindow) {
        mCurrentWindow.setValue(newWindow);
    }

    public LiveData<List<NetworkUsageEntity>> getUsageEntities() { return mCurrentWindowEntities; }
    public LiveData<List<HourlyUsageEntity>> getHourlyUsageEntities() { return mCurrentWindowHourlyEntities; }

    @Override
    protected void onCleared() {
        mCurrentWindowHourlyEntities.stop();
    }

    //Receives all the hourly usage entries from the database and adds the usage information
    private static class HourlyLiveData extends MediatorLiveData<List<HourlyUsageEntity>> {
        private static final long FREQUENCE_BYTE_MEASUREMENT = 1000 * 15; //15 seconds

        private final MutableLiveData<List<HourlyUsageEntity>> mDummyEntry = new MutableLiveData<>(new ArrayList<>());
        private final Handler mHandler;
        private final UsageRetriever mUsageRetriever;
        private final SharedPreferences mSharedPreferences;

        private List<HourlyUsageEntity> mDatabaseEntries = null;


        public HourlyLiveData(LiveData<List<HourlyUsageEntity>> databaseEntries, Application application) {
            mUsageRetriever = new UsageRetriever(application);
            mSharedPreferences = SharedPreferencesHelper.getInstance(application);

            addSource(databaseEntries, networkUsageEntities -> {
                mDatabaseEntries = networkUsageEntities;
                update();
            });

            addSource(mDummyEntry, hourlyUsageEntity -> {
                update();
            });

            mHandler = new Handler(application.getMainLooper());
            mHandler.post(mCreateDummyRunnable);
        }

        private void update() {
            List<HourlyUsageEntity> newList = new ArrayList<>();
            if (mDatabaseEntries != null) newList.addAll(mDatabaseEntries);
            newList.addAll(Objects.requireNonNull(mDummyEntry.getValue()));
            setValue(newList);
        }

        public void stop() {
            mHandler.removeCallbacks(mCreateDummyRunnable);
        }

        private final Runnable mCreateDummyRunnable = new Runnable() {
            @Override
            public void run() {
                //TODO:  Possible issue of time windows when more than a single hour is missing from database.
                long lastCollectedTimestamp = mSharedPreferences.getLong(SharedPreferencesHelper.KEY_LAST_HOURLY_USAGE_TIMESTAMP, -1);

                if (lastCollectedTimestamp != -1) {
                    long endTime = System.currentTimeMillis();
                    long cellularUsage = mUsageRetriever.getDeviceTonnage(NetworkCapabilities.TRANSPORT_CELLULAR, lastCollectedTimestamp, endTime);
                    long wifiUsage = mUsageRetriever.getDeviceTonnage(NetworkCapabilities.TRANSPORT_WIFI, lastCollectedTimestamp, endTime);

//                    Log.d(TAG, "UI: Generating dummy entry for missing data since: " + lastCollectedTimestamp +
//                            "\nCellular usage: " + cellularUsage +
//                            "\nWi-Fi usage: " + wifiUsage);

                    //TODO: Can optimize with fixed list that only changes usage values to avoid instantiating over and over
                    List<HourlyUsageEntity> dummyList = new ArrayList<>();
                    dummyList.add(new HourlyUsageEntity(NetworkCapabilities.TRANSPORT_CELLULAR, cellularUsage, lastCollectedTimestamp));
                    dummyList.add(new HourlyUsageEntity(NetworkCapabilities.TRANSPORT_WIFI, wifiUsage, lastCollectedTimestamp));
                    mDummyEntry.setValue(dummyList);
                }

                mHandler.postDelayed(this, FREQUENCE_BYTE_MEASUREMENT);
            }
        };
    }
}
