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

package io.openschema.mma.example.view;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import io.openschema.mma.example.R;
import io.openschema.mma.example.databinding.ViewTimeSelectorBinding;
import io.openschema.mma.utils.CalendarUtils;


/**
 * Custom View used to filter displayed metrics by time windows.
 */
public class TimeSelector extends ConstraintLayout {

    private static final String TAG = "TimeSelector";

    public enum TimeWindow {
        HOUR,
        DAY,
        MONTH;

        private long mWindowStart = -1;
        private long mWindowEnd = -1;

        @NonNull
        @Override
        public String toString() {
            return this.name().substring(0, 1) + this.name().substring(1).toLowerCase();
        }

        public void calculateWindow() {
            //TODO: consider using a different logic for start and end. (use dates closer to the user's data plan?)
            //Currently
            switch (this) {
                case HOUR:
                    //Calculates time window for the LAST hour. E.g. At 4:35pm, we will get 4:00pm - 5:00pm
                    Calendar hourCal = CalendarUtils.getCurrentHourCalendar();
                    mWindowStart = hourCal.getTimeInMillis();
                    mWindowEnd = mWindowStart + TimeUnit.HOURS.toMillis(1);
                    break;
                case DAY:
                    //Calculates time window for TODAY. E.g. At 4:35pm 7/10, we will get 12:00am 7/10 - 12:00am 7/11
                    Calendar dayCal = CalendarUtils.getCurrentDayCalendar();
                    mWindowStart = dayCal.getTimeInMillis();
                    mWindowEnd = mWindowStart + TimeUnit.DAYS.toMillis(1);
                    break;
                default: //Month
                    //Calculates time window for THIS month. E.g. At 4:35pm 7/10, we will get 12:00am 7/1 - 12:00am 7/31
                    Calendar monthCal = CalendarUtils.getCurrentMonthCalendar();
                    Calendar nextMonthCal = Calendar.getInstance();
                    nextMonthCal.setTimeInMillis(monthCal.getTimeInMillis());
                    nextMonthCal.add(Calendar.MONTH, 1);
                    mWindowStart = monthCal.getTimeInMillis();
                    mWindowEnd = nextMonthCal.getTimeInMillis();
                    break;
            }
        }

        //Make sure you call calculateWindow() first
        public long getWindowStart() { return mWindowStart;}
        public long getWindowEnd() { return mWindowEnd;}
    }

    private TimeWindow mCurrentWindow = TimeWindow.DAY;
    private TimeWindowChangedListener mListener = null;

    private ViewTimeSelectorBinding mBinding;

    public TimeSelector(@NonNull Context context) { this(context, null, 0); }
    public TimeSelector(@NonNull Context context, @Nullable AttributeSet attrs) { this(context, attrs, 0); }
    public TimeSelector(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        if (!isInEditMode()) {
            init(context);
        } else {
            inflate(context, R.layout.view_time_selector, this);
        }
    }

    private void init(Context context) {
        mBinding = ViewTimeSelectorBinding.inflate(LayoutInflater.from(context), this, true);
        mBinding.timeWindowLeft.setOnClickListener(v -> moveUsageWindow(-1));
        mBinding.timeWindowRight.setOnClickListener(v -> moveUsageWindow(1));
        mBinding.setCurrentWindowTxt(mCurrentWindow.toString());
    }

    private void moveUsageWindow(int delta) {
        if (delta == 0) return;

        TimeWindow[] enumValues = TimeWindow.values();
        int currentValue = mCurrentWindow.ordinal();
        currentValue += delta;

        if (currentValue < 0 || currentValue >= enumValues.length) return;

        setTimeWindow(enumValues[currentValue]);
    }

    //TODO: Save selected time window to use it globally around the app
    public void setTimeWindow(TimeWindow newWindow) {
        Log.d(TAG, "UI: Time window has changed to: " + mCurrentWindow.name());
        mCurrentWindow = newWindow;
        mBinding.setCurrentWindowTxt(mCurrentWindow.toString());
        if (mListener != null) mListener.onTimeWindowChanged(mCurrentWindow);
    }

    public void setOnTimeWindowChangedListener(TimeWindowChangedListener listener) {
        mListener = listener;
    }

    public interface TimeWindowChangedListener {
        void onTimeWindowChanged(TimeWindow newWindow);
    }
}
