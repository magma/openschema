package io.openschema.client.util;

import android.content.Context;
import android.util.Log;

import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import io.openschema.client.R;
import io.openschema.client.view.CustomNotification;
import io.openschema.mma.MobileMetricsAgent;
import io.openschema.mma.MobileMetricsService;
import io.openschema.mma.utils.PermissionManager;

// Testing if this method works for keeping our service running long term.
// Worker class called periodically to check if the service is running correctly.
public class ServiceStatusWorker extends Worker {

    private static final String TAG = "ServiceStatusWorker";

    public static final String UNIQUE_PERIODIC_WORKER_NAME = "SERVICE_STATUS_PERIODIC";
    private static final String WORKER_TAG = "SERVICE_STATUS_TAG";

    private final Context mContext;

    public ServiceStatusWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        mContext = context;
    }

    //TODO: centralize this method together with MainActivity and DeviceBootReceiver
    private void startService() {
        //Build OpenSchema agent with required data
        Log.d(TAG, "UI: Building MMA object.");
        MobileMetricsAgent mMobileMetricsAgent = new MobileMetricsAgent.Builder()
                .setAppContext(mContext.getApplicationContext())
                .setCustomNotification(CustomNotification.getInstance(mContext.getApplicationContext()).getNotification())
                .setBackendBaseURL(mContext.getString(R.string.backend_base_url))
                .setBackendCertificateResId(R.raw.backend)
                .setBackendUsername(mContext.getString(R.string.backend_username))
                .setBackendPassword(mContext.getString(R.string.backend_password))
                .build();

        //Start permanent observer to update custom notification. (Only if service isn't running so that we don't create multiple observers on subsequent app starts)
//        if (!MobileMetricsService.isServiceRunning(mContext.getApplicationContext())) {
//            Log.d(TAG, "UI: Setting up observer to update notification.");
////                NetworkQualityViewModel networkQualityViewModel = new ViewModelProvider(context.getApplicationContext()).get(NetworkQualityViewModel.class);
//            NetworkQualityViewModel networkQualityViewModel = new NetworkQualityViewModel((Application) mContext.getApplicationContext());
//
//            networkQualityViewModel.getActiveNetworkQuality().observeForever(networkStatus -> {
//                CustomNotification customNotification = CustomNotification.getInstance(mContext.getApplicationContext());
//
//                //Update the notification's view
//                customNotification.updateNetworkStatus(networkStatus);
//
//                //Update the notification shown by the OS
//                customNotification.show(mContext.getApplicationContext());
//            });
//        }

        //Initialize agent
        Log.d(TAG, "UI: Initializing MMA object.");
        mMobileMetricsAgent.init();
    }

    @NonNull
    @Override
    public Result doWork() {
        if (PermissionManager.areMandatoryPermissionsGranted(mContext)) {
            if (!MobileMetricsService.isServiceRunning(mContext.getApplicationContext())) {
                startService();
            } else {
                Log.d(TAG, "UI: The service is running correctly.");
            }
        } else {
            Log.e(TAG, "UI: The app is missing some permissions and cannot start.");
        }

        return Result.success();
    }

    public static void enqueuePeriodicWorker(Context context) {
        Log.d(TAG, "UI: Enqueuing ServiceStatusWorker");
        PeriodicWorkRequest.Builder workBuilder = new PeriodicWorkRequest.Builder(ServiceStatusWorker.class, 2, TimeUnit.HOURS)
                .addTag(WORKER_TAG);

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(UNIQUE_PERIODIC_WORKER_NAME, ExistingPeriodicWorkPolicy.REPLACE, workBuilder.build());
    }
}
