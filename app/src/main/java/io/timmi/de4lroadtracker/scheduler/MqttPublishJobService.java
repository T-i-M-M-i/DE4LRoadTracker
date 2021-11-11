package io.timmi.de4lroadtracker.scheduler;

import android.annotation.TargetApi;
import android.app.Service;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.RequiresApi;

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class MqttPublishJobService extends JobService {
    private static final String TAG = "MqttPublishJobService";

    @Override
    public boolean onStartJob(JobParameters params) {
        Log.v(TAG, "Job has started!");
        Intent intent = new Intent(this, MqttPublishIntentService.class);
        startService(intent);
        return false;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        Log.v(TAG, "Job has finished!");
        return false;
    }
}