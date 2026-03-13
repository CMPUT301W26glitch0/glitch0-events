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


public class NotificationHelper {

    private static final String LOTTERY_CHANNEL_ID = "lottery_notifications_v2";

    public static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 2001;

    private static final int LOTTERY_NOTIFICATION_ID = 1001;

    private static final int LOTTERY_LOSS_NOTIFICATION_ID = 1002;


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

    public static void showLotteryWinNotification(Context context, String eventName, String eventId) {
        Intent intent = new Intent(context, com.example.cmput301_app.entrant.EventDetailsActivity.class);
        intent.putExtra("eventId", eventId);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, LOTTERY_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("Lottery Update")
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


    public static void handlePermissionResult(Activity activity,
                                              int requestCode,
                                              int[] grantResults,
                                              String eventName) {
        if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showLotteryWinNotification(activity, eventName, "");            }
            else {
                Toast.makeText(activity, "Notification permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public static void showLotteryLossNotification(Context context, String eventName, String eventId) {
        Intent intent = new Intent(context, com.example.cmput301_app.entrant.EventDetailsActivity.class);
        intent.putExtra("eventId", eventId);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                1,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, LOTTERY_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("Lottery Update")
                .setContentText("You were not selected for " + eventName + " in the current draw. You may still be selected if a chosen entrant declines.")
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText("You were not selected for " + eventName + " in the current draw. You may still be selected if a chosen entrant declines."))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_STATUS)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        notificationManager.notify(LOTTERY_LOSS_NOTIFICATION_ID, builder.build());
    }
}