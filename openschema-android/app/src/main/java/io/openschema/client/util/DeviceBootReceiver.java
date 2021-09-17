package io.openschema.client.util;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import io.openschema.client.R;
import io.openschema.client.view.CustomNotification;
import io.openschema.mma.MobileMetricsAgent;
import io.openschema.mma.utils.PermissionManager;

public class DeviceBootReceiver extends BroadcastReceiver {
    private static final String TAG = "DeviceBootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        //Can be called from device boot
        Log.d(TAG, "MMA: Attempting to start the service after device reboot.");

        //TODO: centralize this method together with MainActivity and ServiceStatusWorker
        if (PermissionManager.areMandatoryPermissionsGranted(context)) {
            //Build OpenSchema agent with required data
            Log.d(TAG, "MMA: Building MMA object.");
            MobileMetricsAgent mMobileMetricsAgent = new MobileMetricsAgent.Builder()
                    .setAppContext(context.getApplicationContext())
                    .setCustomNotification(CustomNotification.getInstance(context.getApplicationContext()).getNotification())
                    .setBackendBaseURL(context.getString(R.string.backend_base_url))
                    .setBackendCertificateResId(R.raw.backend)
                    .setBackendUsername(context.getString(R.string.backend_username))
                    .setBackendPassword(context.getString(R.string.backend_password))
                    .build();

            //Start permanent observer to update custom notification. (Only if service isn't running so that we don't create multiple observers on subsequent app starts)
//            if (!MobileMetricsService.isServiceRunning(context.getApplicationContext())) {
//                Log.d(TAG, "MMA: Setting up observer to update notification.");
////                NetworkQualityViewModel networkQualityViewModel = new ViewModelProvider(context.getApplicationContext()).get(NetworkQualityViewModel.class);
//                NetworkQualityViewModel networkQualityViewModel = new NetworkQualityViewModel((Application) context.getApplicationContext());
//
//                networkQualityViewModel.getActiveNetworkQuality().observeForever(networkStatus -> {
//                    CustomNotification customNotification = CustomNotification.getInstance(context.getApplicationContext());
//
//                    //Update the notification's view
//                    customNotification.updateNetworkStatus(networkStatus);
//
//                    //Update the notification shown by the OS
//                    customNotification.show(context.getApplicationContext());
//                });
//            }

            //Initialize agent
            Log.d(TAG, "MMA: Initializing MMA object.");
            mMobileMetricsAgent.init();
        } else {
            Log.e(TAG, "MMA: The app is missing some permissions and cannot start.");
        }
    }
}