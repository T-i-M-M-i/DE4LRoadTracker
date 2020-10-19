package io.timmi.de4lroadtracker;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.menu.ActionMenuItemView;
import androidx.appcompat.view.menu.MenuItemImpl;

import com.google.android.gms.location.LocationRequest;
import com.transistorsoft.locationmanager.adapter.BackgroundGeolocation;
import com.transistorsoft.locationmanager.adapter.TSConfig;
import com.transistorsoft.locationmanager.adapter.callback.TSCallback;
import com.transistorsoft.locationmanager.adapter.callback.TSHeartbeatCallback;
import com.transistorsoft.locationmanager.adapter.callback.TSLocationCallback;
import com.transistorsoft.locationmanager.event.HeartbeatEvent;
import com.transistorsoft.locationmanager.location.TSLocation;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import io.timmi.de4lroadtracker.activity.DebugActivity;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "DE4LMainActivity";
    public static final String HISTORY_MESSAGE_BROADCAST = "io.timmi.de4lroadtracker.historymessagebroadcast";
    public static final String BUFFER_STATUS_BROADCAST = "io.timmi.de4lroadtracker.bufferstatusbroadcast";

    @Nullable
    private BackgroundGeolocation bgGeo = null;

    private void startBG() {
        if(bgGeo  != null) {
            bgGeo.start();
            return;
        }
        bgGeo = BackgroundGeolocation.getInstance(getApplicationContext());
        final TSConfig config = TSConfig.getInstance(getApplicationContext());

        // Configure the SDK
        config.updateWithBuilder()
                .setDebug(true) // Sound Fx / notifications during development
                .setLogLevel(5) // Verbose logging during development
                .setDesiredAccuracy(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setDistanceFilter(10F)
                .setStopTimeout(1L)
                .setHeartbeatInterval(60)
                .setStopOnTerminate(false)
                .setForegroundService(true)
                .setStartOnBoot(true)
                .commit();

        // Listen events
        bgGeo.onLocation(new TSLocationCallback() {
            @Override
            public void onLocation(TSLocation location) {
                Log.i(TAG, "[location] from Main Activity " + location.toJson());
            }
            @Override
            public void onError(Integer code) {
                Log.i(TAG, "[location] ERROR: " + code);
            }
        });

        bgGeo.onMotionChange(new TSLocationCallback() {
            @Override
            public void onLocation(TSLocation tsLocation) {
                Log.i(TAG, "[motionchange] " + tsLocation.toJson());
            }
            @Override
            public void onError(Integer error) {
                Log.i(TAG, "[motionchange] ERROR: " + error);
            }
        });

        bgGeo.onHeartbeat(new TSHeartbeatCallback() {
            @Override
            public void onHeartbeat(HeartbeatEvent heartbeatEvent) {
                Log.i(TAG, "[heartbeat] " + heartbeatEvent.toJson());
            }
        });

        // Finally, signal #ready to the SDK.
        bgGeo.ready(new TSCallback() {
            @Override public void onSuccess() {
                Log.i(TAG, "[ready] success");
                if (!config.getEnabled()) {
                    // Start tracking immediately (if not already).
                } else {
                    Log.d(TAG, "[ready] location services config tells it's already enabled, nevertheless we'll start the service again.");
                }
                try {
                    bgGeo.start();
                } catch (Exception e) {
                    Log.e(TAG, "cannot start background location tracker" + e.getMessage(), e);
                }
            }
            @Override public void onFailure(String error) {
                Log.i(TAG, "[ready] FAILURE: " + error);
            }
        });

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        MenuItem item = menu.findItem(R.id.menu_toggle);
        if(isMyServiceRunning(SensorRecordService.class)) {
            Log.i(TAG, "isMyServiceRunning = true");
            item.setIcon(R.drawable.baseline_stop_black_48);
            item.setTitle(R.string.stop_service);
        } else {
            Log.i(TAG, "isMyServiceRunning = false");
            item.setIcon(R.drawable.baseline_not_started_black_48);
            item.setTitle(R.string.start_service);
        }
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
        startBG();
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