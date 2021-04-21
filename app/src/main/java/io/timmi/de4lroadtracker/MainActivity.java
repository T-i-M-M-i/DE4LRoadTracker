package io.timmi.de4lroadtracker;

import android.Manifest;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.LocationRequest;
import com.transistorsoft.locationmanager.adapter.BackgroundGeolocation;
import com.transistorsoft.locationmanager.adapter.TSConfig;
import com.transistorsoft.locationmanager.adapter.callback.TSCallback;
import com.transistorsoft.locationmanager.adapter.callback.TSLocationCallback;
import com.transistorsoft.locationmanager.location.TSLocation;

import io.timmi.de4lroadtracker.activity.DebugActivity;
import io.timmi.de4lroadtracker.helper.TrackerIndicatorNotification;

public class MainActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener {
    private static final String TAG = "DE4LMainActivity";
    public static final String HISTORY_MESSAGE_BROADCAST = "io.timmi.de4lroadtracker.historymessagebroadcast";
    public static final String BUFFER_STATUS_BROADCAST = "io.timmi.de4lroadtracker.bufferstatusbroadcast";
    private static final int REQUEST_CODE_OPEN_DOCUMENT_TREE = 100;
    private SharedPreferences settings;
    @Nullable
    private MenuItem stopStartMenuItem = null;

    @Nullable
    private BackgroundGeolocation bgGeo;
    @Nullable
    private TSConfig config;

    public MainActivity() {
        //no instance
    }

    private void startBG() {
        if (bgGeo != null) {
            bgGeo.start();
        }
    }

    private void initializeBGLocation() {
        bgGeo = BackgroundGeolocation.getInstance(getApplicationContext());
        config = TSConfig.getInstance(getApplicationContext());

        // Configure the SDK
        TSConfig.Builder builder = config.updateWithBuilder()
                .setDebug(settings.getBoolean("sound", false)) // Sound Fx / notifications during development
                .setLogLevel(5) // Verbose logging during development
                .setDesiredAccuracy(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setDistanceFilter(2F)
                .setStopTimeout(1L)
                .setHeartbeatInterval(60)
                .setStopOnTerminate(false)
                .setForegroundService(true)
                .setStartOnBoot(false);

        String debugUrl = settings.getString("locationServiceUrl", "");

        if (!debugUrl.isEmpty()) {
            builder.setUrl(debugUrl);
        }
        builder.commit();

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

        // Finally, signal #ready to the SDK.
        bgGeo.ready(new TSCallback() {
            @Override
            public void onSuccess() {
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

            @Override
            public void onFailure(String error) {
                Log.i(TAG, "[ready] FAILURE: " + error);
            }
        });

    }
    private void askAndroid10Perm()
    {
        if (Build.VERSION.SDK_INT >= 23) {
            int result = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
            if (result != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
            new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CODE_OPEN_DOCUMENT_TREE);
            }
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        settings = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        askAndroid10Perm();
        initializeBGLocation();
        if(!settings.getBoolean("hasPrivacyAgreement", false)) {
            startActivity(new Intent(this, PrivacyAgreementActivity.class));
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        MenuItem item = menu.findItem(R.id.menu_toggle);
        stopStartMenuItem = item;
        if (isMyServiceRunning(SensorRecordService.class)) {
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

    public void startTrackingService() {
        startBG();
        startService(new Intent(getBaseContext(), SensorRecordService.class));
        if(stopStartMenuItem != null) {
            stopStartMenuItem.setIcon(R.drawable.baseline_stop_black_48);
            stopStartMenuItem.setTitle(R.string.stop_service);
        }
        //moveTaskToBack(true);
        //finish();
    }

    public void stopTrackingService() {
        stopService(new Intent(getBaseContext(), SensorRecordService.class));
        if(stopStartMenuItem != null) {
            stopStartMenuItem.setIcon(R.drawable.baseline_not_started_black_48);
            stopStartMenuItem.setTitle(R.string.start_service);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        String action = intent.getAction();
        if (action == null) {
            return;
        }
        switch (action) {
            case TrackerIndicatorNotification.CLOSE_ACTION:
                stopTrackingService();
                break;
        }
    }

    public void showSettings(MenuItem item) {
        startActivityForResult(new Intent(this, SettingsActivity.class), 0);
    }

    public void showPrivacyAgreement(MenuItem item) {
        startActivity(new Intent(this, PrivacyAgreementActivity.class));
    }

    public void toggleService(MenuItem item) {
        if (isMyServiceRunning(SensorRecordService.class)) {
            stopTrackingService();
        } else {
            startTrackingService();
        }
    }

    public void startMqttTestActivity(MenuItem item) {
        Intent intent = new Intent(getBaseContext(), DebugActivity.class);
        startActivity(intent);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
        if (config == null) {
            return;
        }
        TSConfig.Builder builder = config.updateWithBuilder();
        switch (s) {
            case "sound":
                builder.setDebug(settings.getBoolean("sound", false)); // Sound Fx / notifications during development
                break;
            case "locationServiceUrl":
                String debugUrl = settings.getString("locationServiceUrl", "");
                builder.setUrl(debugUrl);
                break;
            default:
        }
        builder.commit();
    }
}