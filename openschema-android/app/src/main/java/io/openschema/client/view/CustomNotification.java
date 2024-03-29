package io.openschema.client.view;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.os.Bundle;

import androidx.core.app.NotificationCompat;
import androidx.navigation.NavDeepLinkBuilder;
import io.openschema.client.R;
import io.openschema.client.activity.MainActivity;
import io.openschema.mma.utils.PersistentNotification;

public class CustomNotification {

    private static final String TAG = "CustomNotification";
    private static CustomNotification _instance = null;

    public static CustomNotification getInstance(Context context) {
        if (_instance == null) {
            synchronized (PersistentNotification.class) {
                if (_instance == null) {
                    _instance = new CustomNotification(context.getApplicationContext());
                }
            }
        }
        return _instance;
    }

    private NotificationCompat.Builder mCustomNotificationBuilder = null;
//    private RemoteViews mNotificationView;

    private CustomNotification(Context context) {
        init(context);
//        updateNetworkStatus(null);
    }

    private void init(Context context) {
        //Create intent to open main page
        Bundle args = new Bundle();
        args.putInt("requestCode", PersistentNotification.SERVICE_NOTIFICATION_ID);
        PendingIntent pendingIntent = new NavDeepLinkBuilder(context)
                .setComponentName(MainActivity.class)
                .setGraph(R.navigation.nav_graph_main)
                .setDestination(R.id.nav_usage)
                .setArguments(args)
                .createPendingIntent();

        //TODO: Custom notification was disabled due to sometimes not updating correctly.
        // We might need to implement a bound-service architecture for the notification to have a lifecycle to update from.

//        mNotificationView = new RemoteViews(context.getPackageName(), R.layout.view_custom_notification);

//        mCustomNotificationBuilder = new NotificationCompat.Builder(context, PersistentNotification.SERVICE_NOTIFICATION_CHANNEL_ID)
//                .setSmallIcon(io.openschema.mma.R.drawable.ic_persistent_notification)
//                .setCustomContentView(mNotificationView)
//                .setStyle(new NotificationCompat.DecoratedCustomViewStyle())
//                .setShowWhen(false)
//                .setCategory(NotificationCompat.CATEGORY_SERVICE)
//                .setPriority(NotificationCompat.PRIORITY_LOW)
//                .setContentIntent(pendingIntent);

        mCustomNotificationBuilder = new NotificationCompat.Builder(context, PersistentNotification.SERVICE_NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(io.openschema.mma.R.drawable.ic_persistent_notification)
                .setContentTitle("OpenSchema is running")
                .setContentText("Tap here for more information.")
                .setOngoing(true) //notification can't be swiped
                .setShowWhen(false)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setContentIntent(pendingIntent);
    }

    public Notification getNotification() {
        return mCustomNotificationBuilder.build();
    }

//    public void updateNetworkStatus(NetworkQualityView.NetworkStatus networkStatus) {
//        if (networkStatus != null) {
//            if (networkStatus.mTransportType != NetworkQualityView.NetworkStatus.MEASURING_NETWORK) {
//                int networkIcon = networkStatus.mTransportType == NetworkCapabilities.TRANSPORT_WIFI ? R.drawable.ic_notification_network_wifi : R.drawable.ic_notification_network_cellular;
//
//                String networkPrimary = "Current Network: ";
//                networkPrimary += networkStatus.mTransportType == NetworkCapabilities.TRANSPORT_WIFI ? "Wi-Fi" : "Cellular";
//
//                String networkQuality = "Detected Quality: " + networkStatus.getNetworkQualityString();
//
//                mNotificationView.setImageViewResource(R.id.notification_network_primary_icon, networkIcon);
//                mNotificationView.setTextViewText(R.id.notification_network_primary, networkPrimary);
//                mNotificationView.setTextViewText(R.id.notification_network_quality, networkQuality);
//            } else {
//                mNotificationView.setImageViewResource(R.id.notification_network_primary_icon, R.drawable.ic_notification_network_unknown);
//                mNotificationView.setTextViewText(R.id.notification_network_primary, "Active network is being measured.");
//                mNotificationView.setTextViewText(R.id.notification_network_quality, "");
//            }
//        } else {
//            mNotificationView.setImageViewResource(R.id.notification_network_primary_icon, R.drawable.ic_notification_network_unknown);
//            mNotificationView.setTextViewText(R.id.notification_network_primary, "No active network detected.");
//            mNotificationView.setTextViewText(R.id.notification_network_quality, "");
//        }
//    }

//    public void show(Context context) {
//        NotificationManagerCompat.from(context)
//                .notify(PersistentNotification.SERVICE_NOTIFICATION_ID, mCustomNotificationBuilder.build());
//    }
}
