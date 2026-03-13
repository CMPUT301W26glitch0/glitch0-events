package com.example.cmput301_app.entrant;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.example.cmput301_app.R;

/*
 * Helper class for creating notification channels, requesting notification permission,
 * and showing lottery win notifications.
 */
public class NotificationHelper {

    // ID used by Android to group lottery notifications into a channel
    private static final String LOTTERY_CHANNEL_ID = "lottery_notifications_v2";

    // Request code used when asking for notification permission on Android 13+
    public static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 2001;

    // Unique integer ID for this notification
    private static final int LOTTERY_NOTIFICATION_ID = 1001;

    /*
     * Creates the notification channel required for Android 8+.
     * High importance is used so the notification has a better chance
     * of appearing as a heads-up popup.
     */
    public static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Lottery Notifications";
            String description = "Notifications for lottery results";
            int importance = NotificationManager.IMPORTANCE_HIGH;

            NotificationChannel channel = new NotificationChannel(
                    LOTTERY_CHANNEL_ID,
                    name,
                    importance
            );
            channel.setDescription(description);

            NotificationManager notificationManager =
                    context.getSystemService(NotificationManager.class);

            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    /*
     * Checks notification permission on Android 13+.
     * If permission is granted, shows the demo lottery notification.
     * If not, requests permission from the user.
     */
    public static void requestNotificationPermissionAndShowDemo(Activity activity, String eventName) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS)
                    == PackageManager.PERMISSION_GRANTED) {
                showLotteryWinNotification(activity, eventName);
            } else {
                ActivityCompat.requestPermissions(
                        activity,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        NOTIFICATION_PERMISSION_REQUEST_CODE
                );
            }
        } else {
            showLotteryWinNotification(activity, eventName);
        }
    }

    /*
     * Shows a lottery win notification.
     * Tapping the notification reopens the DashboardActivity for now.
     * Later this can be changed to open a dedicated invitation screen.
     */
    public static void showLotteryWinNotification(Context context, String eventName) {
        Intent intent = new Intent(context, com.example.cmput301_app.entrant.DashboardActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, LOTTERY_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("Lottery Complete")
                .setContentText("You were selected for " + eventName + ". Tap to view your invitation.")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_EVENT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        notificationManager.notify(LOTTERY_NOTIFICATION_ID, builder.build());
    }

    /*
     * Handles the result of the runtime notification permission request.
     * If granted, the demo notification is shown.
     */
    public static void handlePermissionResult(Activity activity,
                                              int requestCode,
                                              int[] grantResults,
                                              String eventName) {
        if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showLotteryWinNotification(activity, eventName);
            } else {
                Toast.makeText(activity, "Notification permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
}