package io.timmi.de4lroadtracker;

import android.Manifest;
import android.app.ActivityManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import io.timmi.de4lroadtracker.activity.DebugActivity;
import io.timmi.de4lroadtracker.helper.Md5Builder;
import io.timmi.de4lroadtracker.helper.RawResourceLoader;
import io.timmi.de4lroadtracker.helper.TrackerIndicatorNotification;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class MainActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener {
    private static final String TAG = "DE4LMainActivity";
    public static final String HISTORY_MESSAGE_BROADCAST = "io.timmi.de4lroadtracker.historymessagebroadcast";
    private static final int REQUEST_CODE_OPEN_DOCUMENT_TREE = 100;
    private SharedPreferences settings;
    @Nullable
    private MenuItem stopStartMenuItem = null;

    public MainActivity() {
        //no instance
    }

    private void showDialogIfNoLocationPermission(final Runnable proceedTask) {
        boolean canAccessLocation = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED;
        if (!canAccessLocation) {
            new MaterialAlertDialogBuilder(this)
                    .setMessage(R.string.locationUsageAlertMessage)
                    .setPositiveButton(R.string.proceed, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            proceedTask.run();
                        }
                    })
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {}
                    })
                    .show();
        } else {
            proceedTask.run();
        }
    }

    private void askAndroid10Perm() {
        if (Build.VERSION.SDK_INT >= 23) {
            int result = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
            if (result != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CODE_OPEN_DOCUMENT_TREE);
            }
        }
    }

    private void updateView() {
        TextView amountOfWaypoints = (TextView) findViewById(R.id.amountOfWaypoints);
        amountOfWaypoints.setText(
                String.valueOf(settings.getLong("messagesDelivered", 0))
        );
        TextView distanceView = (TextView) findViewById(R.id.distance);
        String distanceTracked =
                String.valueOf(settings.getLong("distanceMeterTracked", 0) / 1000) + " km";
        distanceView.setText(distanceTracked);
    }

    private boolean privacyAgreementAccepted() {
        String agreementText = RawResourceLoader.loadText(R.raw.privacy, this);
        SharedPreferences sharedPreferences = androidx.preference.PreferenceManager.getDefaultSharedPreferences(this);
        if (sharedPreferences.contains("agreedPrivacyMD5") && agreementText != null) {
            try {
                return Md5Builder.md5(agreementText).equals(sharedPreferences.getString("agreedPrivacyMD5", ""));
            } catch (Exception e) {
            }
        }
        return false;

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        settings = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        settings.registerOnSharedPreferenceChangeListener(this);
        ((TextView) findViewById(R.id.firstParagraph)).setMovementMethod(LinkMovementMethod.getInstance());
        ((TextView) findViewById(R.id.privacyHint)).setMovementMethod(LinkMovementMethod.getInstance());
        updateView();
        askAndroid10Perm();
        if (!privacyAgreementAccepted()) {
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
        settings.unregisterOnSharedPreferenceChangeListener(this);
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
        showDialogIfNoLocationPermission(new Runnable() {
            @Override
            public void run() {
                startService(new Intent(getBaseContext(), SensorRecordService.class));
                if (stopStartMenuItem != null) {
                    stopStartMenuItem.setIcon(R.drawable.baseline_stop_black_48);
                    stopStartMenuItem.setTitle(R.string.stop_service);
                }
                //moveTaskToBack(true);
                //finish();
            }
        });
    }

    public void stopTrackingService() {
        stopService(new Intent(getBaseContext(), SensorRecordService.class));
        if (stopStartMenuItem != null) {
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
        switch (s) {
            case "distanceMeterTracked":
            case "messagesDelivered":
                updateView();
                break;
            default:
        }
    }
}