package io.timmi.de4lroadtracker;

import android.Manifest;
import android.app.ActivityManager;
import androidx.appcompat.app.AlertDialog;

import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.preference.PreferenceManager;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import com.google.android.material.textfield.TextInputEditText;
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

    private void showPasswordAlertIfNoPassword(final Runnable proceedTask) {
        if (settings.getString("mqttPW", "").isEmpty()) {
            new MaterialAlertDialogBuilder(this)
                    .setMessage(R.string.passwordAlert)
                    .setView(R.layout.password_entry_view)
                    .setPositiveButton(R.string.proceed, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            String password = ((TextInputEditText) ((AlertDialog) dialogInterface).findViewById(R.id.editMqttPW)).getText().toString();
                            settings.edit()
                                    .putString("mqttPW", password)
                                    .apply();
                            proceedTask.run();
                        }
                    })
                    .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                        }
                    })
                    .show();
        } else {
            proceedTask.run();
        }
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
                        public void onClick(DialogInterface dialogInterface, int i) {
                        }
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
        TextView servicesRunningCount = (TextView) findViewById(R.id.servicesRunningCount);
        String servicesRunningCountText = String.valueOf(settings.getInt("servicesRunning", 0));
        servicesRunningCount.setText(servicesRunningCountText);
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
        doUnbindService();
        settings.unregisterOnSharedPreferenceChangeListener(this);
        //unregisterReceiver();
    }

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        return SensorRecordService.isRunning;
    }

    public void startTrackingService() {
        showPasswordAlertIfNoPassword(new Runnable() {
            @Override
            public void run() {
                showDialogIfNoLocationPermission(new Runnable() {
                    @Override
                    public void run() {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            startForegroundService(new Intent(getBaseContext(), SensorRecordService.class));
                        } else {
                            startService(new Intent(getBaseContext(), SensorRecordService.class));
                        }
                        if (stopStartMenuItem != null) {
                            stopStartMenuItem.setIcon(R.drawable.baseline_stop_black_48);
                            stopStartMenuItem.setTitle(R.string.stop_service);
                        }
                        doBindService();
                        //moveTaskToBack(true);
                        //finish();
                    }
                });
            }
        });
    }

    public void stopTrackingService() {

        if( mService != null ) {
            try {
                mService.send(Message.obtain(null,
                        SensorRecordService.STOP_SERVICE_MESSAGE));
                doUnbindService();
                if (stopStartMenuItem != null) {
                    stopStartMenuItem.setIcon(R.drawable.baseline_not_started_black_48);
                    stopStartMenuItem.setTitle(R.string.start_service);
                }
            } catch (Exception e) {
                Log.e(TAG, "Cannot stop service, message could not be sent");
            }
        }
        //stopService(new Intent(getBaseContext(), SensorRecordService.class));
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
            case "servicesRunning":
                updateView();
                break;
            default:
        }
    }


    Messenger mService = null;
    boolean mIsBound;

    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            mService = new Messenger(service);
        }

        public void onServiceDisconnected(ComponentName className) {
            mService = null;
        }
    };

    void doBindService() {
        bindService(new Intent(this,
                SensorRecordService.class), mConnection, Context.BIND_AUTO_CREATE);
        mIsBound = true;
    }

    void doUnbindService() {
        if (mIsBound) {
            unbindService(mConnection);
            mIsBound = false;
        }
    }

}