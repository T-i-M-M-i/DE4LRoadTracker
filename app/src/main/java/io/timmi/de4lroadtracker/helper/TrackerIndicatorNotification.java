package io.timmi.de4lroadtracker.helper;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.MediaSession2Service;
import android.os.Build;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import io.timmi.de4lroadtracker.MainActivity;
import io.timmi.de4lroadtracker.R;

import static android.content.Context.NOTIFICATION_SERVICE;

public class TrackerIndicatorNotification {
    private static final int NOTIFICATION = 1;
    private static final String NOTIFICATION_CHANNEL_ID = "20001";
    public static final String CLOSE_ACTION = "close";
    @Nullable
    private NotificationManager mNotificationManager = null;
    Context ctx;
    NotificationCompat.Builder mNotificationBuilder;


    public TrackerIndicatorNotification(Context ctx) {
        this.ctx = ctx;
        mNotificationBuilder = new NotificationCompat.Builder(ctx, NOTIFICATION_CHANNEL_ID);
    }


    public void setupNotifications() { //called in onCreate()
        if (mNotificationManager == null) {
            mNotificationManager = (NotificationManager) ctx.getSystemService(NOTIFICATION_SERVICE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel channel = new NotificationChannel(
                        NOTIFICATION_CHANNEL_ID,
                        "DE4L Tracking Activity",
                        NotificationManager.IMPORTANCE_HIGH);
                mNotificationManager.createNotificationChannel(channel);
            }
        }


        PendingIntent pendingIntent = PendingIntent.getActivity(ctx, 0,
                new Intent(ctx, MainActivity.class)
                        .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP),
                0);
        PendingIntent pendingCloseIntent = PendingIntent.getActivity(ctx, 0,
                new Intent(ctx, MainActivity.class)
                        .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP)
                        .setAction(CLOSE_ACTION),
                0);
        mNotificationBuilder
                .setSmallIcon(R.drawable.ic_launcher_background)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setContentTitle(ctx.getText(R.string.app_name))
                .setWhen(System.currentTimeMillis())
                .setContentIntent(pendingIntent)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel,
                        ctx.getString(R.string.action_exit), pendingCloseIntent)
                .setOngoing(true);
    }

    public void showNotification() {
        mNotificationBuilder
                .setTicker(ctx.getText(R.string.service_connected))
                .setContentText(ctx.getText(R.string.service_connected));
        if (mNotificationManager != null) {
            mNotificationManager.notify(NOTIFICATION, mNotificationBuilder.build());
        }
    }

    public void closeNotification() {
        if (mNotificationManager != null) {
            mNotificationManager.cancel(NOTIFICATION);
        }
    }
}
