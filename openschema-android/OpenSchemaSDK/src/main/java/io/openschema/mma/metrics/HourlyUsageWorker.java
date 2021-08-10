package io.openschema.mma.metrics;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.NetworkCapabilities;
import android.util.Log;

import java.time.Instant;
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
import io.openschema.mma.metrics.collectors.NetworkHourlyMetrics;
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
    private final NetworkHourlyMetrics mNetworkHourlyMetrics;
    private MetricsManager mMetricsManager;

    public HourlyUsageWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        Log.d(TAG, "MMA: Initializing HourlyUsageWorker");
        mSharedPreferences = SharedPreferencesHelper.getInstance(context);
        mUsageRetriever = new UsageRetriever(context);
        mMetricsRepository = MetricsRepository.getRepository(context.getApplicationContext());
        mNetworkHourlyMetrics = new NetworkHourlyMetrics(context);
        mMetricsManager = new MetricsManager(getApplicationContext());
    }

    private void collectData() {
        //Get last collected timestamp from persistent storage
        //Ideally we would only count from the instant the app was installed, but we are limited to hour-level granularity with NetworkStatsManager
        long lastCollectedTimestamp = mSharedPreferences.getLong(SharedPreferencesHelper.KEY_LAST_HOURLY_USAGE_TIMESTAMP, -1);
        if (lastCollectedTimestamp == -1) {
            //First time worker is called after installation
            //Initialize the value to the current hour.
            lastCollectedTimestamp = CalendarUtils.getCurrentHourCalendar().getTimeInMillis();
        }

        //Calculate amount of iterations pending
        Instant lastCollectedInstant = Instant.ofEpochMilli(lastCollectedTimestamp);
        Log.d(TAG, "MMA: Last collected hour: " + lastCollectedInstant.toString());
        long hourSegments = ChronoUnit.HOURS.between(lastCollectedInstant, Instant.ofEpochMilli(System.currentTimeMillis()));
        Log.d(TAG, "MMA: Collecting " + hourSegments + " hours: ");

        //Iterate over each hour missing from DB
        Calendar currentSegmentStart = Calendar.getInstance();
        currentSegmentStart.setTimeInMillis(lastCollectedTimestamp);
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
        //Collect information to write to local db for UI
        mMetricsRepository.writeHourlyUsage(getUsageEntity(NetworkCapabilities.TRANSPORT_WIFI, segmentStart, segmentEnd));
        mMetricsRepository.writeHourlyUsage(getUsageEntity(NetworkCapabilities.TRANSPORT_CELLULAR, segmentStart, segmentEnd));

        //Collect information to write to metrics db for uploading
        mMetricsManager.collect(NetworkHourlyMetrics.METRIC_NAME, mNetworkHourlyMetrics.retrieveMetrics(NetworkCapabilities.TRANSPORT_WIFI, segmentStart.getTimeInMillis(), segmentEnd.getTimeInMillis()));
        mMetricsManager.collect(NetworkHourlyMetrics.METRIC_NAME, mNetworkHourlyMetrics.retrieveMetrics(NetworkCapabilities.TRANSPORT_CELLULAR, segmentStart.getTimeInMillis(), segmentEnd.getTimeInMillis()));
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
