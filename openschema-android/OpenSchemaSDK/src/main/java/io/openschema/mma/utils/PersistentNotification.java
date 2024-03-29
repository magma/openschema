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

package io.openschema.mma.utils;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import io.openschema.mma.R;

public class PersistentNotification {

    private static final String TAG = "PersistentNotification";
    public static final int SERVICE_NOTIFICATION_ID = 1;
    public static final String SERVICE_NOTIFICATION_CHANNEL_ID = "SERVICE_NOTIFICATION_CHANNEL";

    private static PersistentNotification _instance = null;

    private Notification mPersistentNotification = null;

    public static PersistentNotification getInstance(Context context) {
        if (_instance == null) {
            synchronized (PersistentNotification.class) {
                if (_instance == null) {
                    _instance = new PersistentNotification(context.getApplicationContext());
                }
            }
        }
        return _instance;
    }

    private PersistentNotification(Context context) {
        Log.d(TAG, "UI: Creating PersistentNotification");
        initNotificationChannel(context);
        initNotificationBuilder(context);
    }

    private void initNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Persistent Service";
            String description = "Active when application is running";
            int importance = NotificationManager.IMPORTANCE_LOW;
            NotificationChannel channel = new NotificationChannel(SERVICE_NOTIFICATION_CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void initNotificationBuilder(Context context) {
        //Create intent to open app details
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.fromParts("package", context.getPackageName(), null));
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        mPersistentNotification = new NotificationCompat.Builder(context, SERVICE_NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_persistent_notification)
                .setContentTitle("OpenSchema is running")
                .setContentText("Tap here for more information.")
                .setOngoing(true) //notification can't be swiped
                .setShowWhen(false)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setContentIntent(pendingIntent)
                .build();
    }

    public void show(Context context) {
        NotificationManagerCompat.from(context)
                .notify(SERVICE_NOTIFICATION_ID, mPersistentNotification);
    }

    public void hide(Context context) {
        NotificationManagerCompat.from(context)
                .cancel(SERVICE_NOTIFICATION_ID);
    }

    public void setCustomNotification(Notification notification) {
        mPersistentNotification = notification;
    }

    public Notification getNotification() {
        return mPersistentNotification;
    }
}
