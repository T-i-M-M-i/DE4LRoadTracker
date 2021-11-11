package io.timmi.de4lroadtracker.scheduler;

import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.util.Log;

import androidx.legacy.content.WakefulBroadcastReceiver;

public class AlarmWakefulReceiver  extends WakefulBroadcastReceiver {

    private static final String TAG = "AlarmWakefulReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {

        Intent service = new Intent(context, MqttPublishWakefulService.class);
        Log.i(TAG, "Starting service @ " + SystemClock.elapsedRealtime());

        startWakefulService(context, service);
    }
}
