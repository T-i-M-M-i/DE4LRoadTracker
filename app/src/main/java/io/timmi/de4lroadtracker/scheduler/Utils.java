package io.timmi.de4lroadtracker.scheduler;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.RequiresApi;

public class Utils {

    private static final String TAG = "Utils";

    private static final int ALARM_ID = 100;
    private static final int JOB_ID = 200;


    public static void setAlarmManager(Context context) {
        Log.v(TAG, "Alarm Manager is starting");
        Intent mqttPublisherIntent = new Intent(context, AlarmWakefulReceiver.class);
        @SuppressLint("UnspecifiedImmutableFlag") PendingIntent pendingintent = PendingIntent.getBroadcast(context, ALARM_ID, mqttPublisherIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        AlarmManager alarmManager = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime(), Constants.TIME_INTERVAL, pendingintent);
    }


    public static void cancelAlarm(Context context){
        Intent mqttPublisherIntent = new Intent(context, AlarmWakefulReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, ALARM_ID, mqttPublisherIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        AlarmManager alarmManager =  (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(pendingIntent);

    }


    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public static void setJobScheduler(Context context) {
        Log.v(TAG, "Job Scheduler is starting");
        JobScheduler jobScheduler = (JobScheduler)context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        @SuppressLint("JobSchedulerService") ComponentName serviceName = new ComponentName(context, MqttPublishJobService.class);
        JobInfo jobInfo;
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N){
            jobInfo = new JobInfo.Builder(JOB_ID, serviceName)
                    .setPeriodic(Constants.TIME_INTERVAL_FOR_N)
                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                    .build();
        }else{
            jobInfo = new JobInfo.Builder(JOB_ID, serviceName)
                    .setPeriodic(Constants.TIME_INTERVAL)
                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                    .build();
        }
        jobScheduler.schedule(jobInfo);
    }


    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public static void cancelJobScheduler(Context context){
        JobScheduler jobScheduler = (JobScheduler)context.getApplicationContext().getSystemService(Context.JOB_SCHEDULER_SERVICE);
        jobScheduler.cancel(JOB_ID);
    }
}
