package io.timmi.de4lroadtracker;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import io.timmi.de4lroadtracker.activity.MQTTActivity;

public class MainActivity extends AppCompatActivity {
    private static String TAG = "DE4LMainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

    }

    public void startService(View view) {
        startService(new Intent(getBaseContext(), SensorRecordService.class));
        //moveTaskToBack(true);
        //finish();
    }
    // Method to stop the service
    public void stopService(View view) {
        stopService(new Intent(getBaseContext(), SensorRecordService.class));
    }

    public void mqttTest(View view) {
        Intent intent  = new Intent(getBaseContext(), MQTTActivity.class);
        startActivity(intent);
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

}