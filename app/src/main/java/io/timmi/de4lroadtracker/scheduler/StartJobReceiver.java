package io.timmi.de4lroadtracker.scheduler;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;


/**
 * Called  at the beginning of the job or after the app has been awakened
 *
 * Will either (>= Lollipop) start a JobScheduler or a AlarmManager
 */
public class StartJobReceiver extends BroadcastReceiver {

    public static final String START_JOB_BROADCAST_RECEIVER = "io.timmi.de4lroadtracker.intent.action.START_JOB_FIRSTTIME";
    private static final String TAG = "StartJobReceiver";
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.v(TAG, "StartJobReceiver starting");
        Toast.makeText(context, "Intent Detected.", Toast.LENGTH_LONG).show();
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            Utils.setJobScheduler(context);
        else
            Utils.setAlarmManager(context);
    }
}