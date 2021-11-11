package io.timmi.de4lroadtracker.scheduler;

import android.content.Intent;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.Nullable;

/**
 * Used by the AlarmManager
 */
public class MqttPublishWakefulService extends MqttPublishIntentService {

    private static final String TAG = "MqttPublishWakefulSrv";

    @Override protected void onHandleIntent(@Nullable Intent intent) {
        super.onHandleIntent(intent);
        Log.i(TAG, "Completed service @ " + SystemClock.elapsedRealtime());
        if(intent!=null)
            AlarmWakefulReceiver.completeWakefulIntent(intent);
    }

}
