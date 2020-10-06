package io.timmi.de4lroadtracker;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import io.timmi.de4lroadtracker.activity.DebugActivity;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "DE4LMainActivity";
    public static final String HISTORY_MESSAGE_BROADCAST = "io.timmi.de4lroadtracker.historymessagebroadcast";
    public static final String BUFFER_STATUS_BROADCAST = "io.timmi.de4lroadtracker.bufferstatusbroadcast";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //unregisterReceiver();
    }

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    public void startService() {
        startService(new Intent(getBaseContext(), SensorRecordService.class));
        //moveTaskToBack(true);
        //finish();
    }

    public void stopService() {
        stopService(new Intent(getBaseContext(), SensorRecordService.class));
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        String action = intent.getAction();
        if (action == null) {
            return;
        }
        switch (action) {
            case SensorRecordService.CLOSE_ACTION:
                stopService(new Intent(this, SensorRecordService.class));
                break;
        }
    }

    public void showSettings(MenuItem item) {
        startActivityForResult(new Intent(this, SettingsActivity.class), 0);
    }

    public void toggleService(MenuItem item) {
        if(isMyServiceRunning(SensorRecordService.class)) {
            stopService();
            item.setIcon(R.drawable.baseline_not_started_black_48);
            item.setTitle(R.string.start_service);
        } else {
            startService();
            item.setIcon(R.drawable.baseline_stop_black_48);
            item.setTitle(R.string.stop_service);
        }
    }


    public void startMqttTestActivity(MenuItem item) {
        Intent intent  = new Intent(getBaseContext(), DebugActivity.class);
        startActivity(intent);
    }
}