package io.openschema.mma.metrics;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.NetworkCapabilities;
import android.util.Log;

import java.time.temporal.ChronoUnit;
import java.util.Calendar;
import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import io.openschema.mma.data.MetricsRepository;
import io.openschema.mma.data.entity.HourlyUsageEntity;
import io.openschema.mma.utils.CalendarUtils;
import io.openschema.mma.utils.SharedPreferencesHelper;
import io.openschema.mma.utils.UsageRetriever;

public class HourlyUsageWorker extends Worker {

    private static final String TAG = "HourlyUsageWorker";

    public static final String UNIQUE_PERIODIC_WORKER_NAME = "HOURLY_USAGE_PERIODIC";
    private static final String WORKER_TAG = "HOURLY_USAGE_TAG";

    private final SharedPreferences mSharedPreferences;
    private final UsageRetriever mUsageRetriever;
    private final MetricsRepository mMetricsRepository;

    public HourlyUsageWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        Log.d(TAG, "MMA: Initializing HourlyUsageWorker");
        mSharedPreferences = SharedPreferencesHelper.getInstance(context);
        mUsageRetriever = new UsageRetriever(context);
        mMetricsRepository = MetricsRepository.getRepository(context.getApplicationContext());
    }

    private void collectData() {
        //Get last collected timestamp from persistent storage
        long lastCollectedTimestamp = mSharedPreferences.getLong(SharedPreferencesHelper.KEY_LAST_HOURLY_USAGE_TIMESTAMP, -1);
        if (lastCollectedTimestamp == -1) {
            //First time worker is called after installation
            //Initialize the value to the current hour.
            lastCollectedTimestamp = CalendarUtils.getCurrentHourCalendar().getTimeInMillis();
        }

        //Convert to Calendar object
        Calendar lastCollectedCal = Calendar.getInstance();
        lastCollectedCal.setTimeInMillis(lastCollectedTimestamp);
        Log.d(TAG, "MMA: Last collected hour: " + lastCollectedCal.getTime().toString());

        //Calculate amount of iterations pending
        Calendar currentHourCal = CalendarUtils.getCurrentHourCalendar();
        long hourSegments = ChronoUnit.HOURS.between(lastCollectedCal.toInstant(), currentHourCal.toInstant());

        Log.d(TAG, "MMA: Collecting " + hourSegments + " hours: ");

        Calendar currentSegmentStart = Calendar.getInstance();
        currentSegmentStart.setTimeInMillis(lastCollectedCal.getTimeInMillis());
        for (int i = 0; i < hourSegments; i++) {
            Calendar segmentStart = Calendar.getInstance();
            Calendar segmentEnd = Calendar.getInstance();

            segmentStart.setTimeInMillis(currentSegmentStart.getTimeInMillis());


            segmentEnd.setTimeInMillis(segmentStart.getTimeInMillis());
            segmentEnd.add(Calendar.HOUR_OF_DAY, 1);

            //Retrieve stats within this segment and push it
            collectSegment(segmentStart, segmentEnd);

            //Configure variables for next iteration
            currentSegmentStart.setTimeInMillis(segmentEnd.getTimeInMillis());
        }

        //Save the last hour segment into persistent storage
        mSharedPreferences.edit()
                .putLong(SharedPreferencesHelper.KEY_LAST_HOURLY_USAGE_TIMESTAMP, currentSegmentStart.getTimeInMillis())
                .apply();
    }

    private void collectSegment(Calendar segmentStart, Calendar segmentEnd) {
        Log.d(TAG, "MMA: Collecting Segment: " + segmentStart.getTime().toString() + " | " + segmentEnd.getTime().toString());
        mMetricsRepository.writeHourlyUsage(getUsageEntity(NetworkCapabilities.TRANSPORT_WIFI, segmentStart, segmentEnd));
        mMetricsRepository.writeHourlyUsage(getUsageEntity(NetworkCapabilities.TRANSPORT_CELLULAR, segmentStart, segmentEnd));
    }

    private HourlyUsageEntity getUsageEntity(int transportType, Calendar segmentStart, Calendar segmentEnd) {
        long usage = mUsageRetriever.getDeviceTonnage(transportType, segmentStart.getTimeInMillis(), segmentEnd.getTimeInMillis());
        return new HourlyUsageEntity(transportType, usage, segmentStart.getTimeInMillis());
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "MMA: Running HourlyUsageWorker job");

        collectData();

        Log.d(TAG, "MMA: HourlyUsageWorker job was completed");
        return Result.success();
    }

    public static void enqueuePeriodicWorker(Context context) {
        Log.d(TAG, "MMA: Enqueuing HourlyUsageWorker");
        PeriodicWorkRequest.Builder workBuilder = new PeriodicWorkRequest.Builder(HourlyUsageWorker.class, 1, TimeUnit.HOURS)
                .addTag(WORKER_TAG);

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(UNIQUE_PERIODIC_WORKER_NAME, ExistingPeriodicWorkPolicy.REPLACE, workBuilder.build());
    }
}
