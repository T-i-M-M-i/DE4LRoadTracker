package io.timmi.de4lroadtracker;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.PowerManager;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONArray;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import io.timmi.de4lfilter.Filter;
import io.timmi.de4lroadtracker.activity.DebugActivity;
import io.timmi.de4lroadtracker.helper.Md5Builder;
import io.timmi.de4lroadtracker.helper.Publisher;
import io.timmi.de4lroadtracker.helper.RawResourceLoader;
import io.timmi.de4lroadtracker.helper.TSLocationWrapper;
import io.timmi.de4lroadtracker.helper.TrackerIndicatorNotification;

public class MainActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener {
    private static final String TAG = "DE4LMainActivity";
    public static final String HISTORY_MESSAGE_BROADCAST = "io.timmi.de4lroadtracker.historymessagebroadcast";
    private static final int REQUEST_CODE_OPEN_DOCUMENT_TREE = 100;
    private SharedPreferences settings;
    @Nullable
    private MenuItem stopStartMenuItem = null;
    private TSLocationWrapper locationService = null;
    @Nullable
    private MQTTConnection mqttConnection = null;

    private MQTTConnection getMqttConnection() {
        if (mqttConnection == null) {
            mqttConnection = new MQTTConnection(getApplicationContext(), getApplicationContext());
        }
        return mqttConnection;
    }

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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int result = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
            if (result != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CODE_OPEN_DOCUMENT_TREE);
            }
        }
    }

    @SuppressLint("BatteryLife")
    private void askBatteryOptimizationExclusion() {
        String packageName = getPackageName();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
            boolean isIgnoringBatteryOptimizations = pm.isIgnoringBatteryOptimizations(packageName);
            if (!isIgnoringBatteryOptimizations) {
                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                intent.setData( Uri.parse("package:" + packageName) );
                startActivityForResult(intent, 0);
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


    public void filterAndPublish(MenuItem item) {
        filterAndPublish();
    }

    public void filterAndPublish() {
        Publisher.filterAndPublish(getExternalFilesDir(null), getApplicationContext(), getMqttConnection());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        String filterTest = Filter.filterMany("[[], []]", "[{}, {}]", "{}", "{}");
        if(filterTest == null) {
           Log.i(TAG, "filter returned null (success)");
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        settings = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        settings.registerOnSharedPreferenceChangeListener(this);
        ((TextView) findViewById(R.id.firstParagraph)).setMovementMethod(LinkMovementMethod.getInstance());
        ((TextView) findViewById(R.id.privacyHint)).setMovementMethod(LinkMovementMethod.getInstance());
        locationService = new TSLocationWrapper(getApplicationContext());
        updateView();
        askAndroid10Perm();
        if (!privacyAgreementAccepted()) {
            startActivity(new Intent(this, PrivacyAgreementActivity.class));
        }
        getMqttConnection();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        MenuItem item = menu.findItem(R.id.menu_toggle);
        stopStartMenuItem = item;
        if (isMyServiceRunning(SensorRecorder.class)) {
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
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    public void startTrackingService() {
        showPasswordAlertIfNoPassword(new Runnable() {
            @Override
            public void run() {
                showDialogIfNoLocationPermission(new Runnable() {
                    @Override
                    public void run() {
                        startService(new Intent(getBaseContext(), SensorRecorder.class));
                        if (stopStartMenuItem != null) {
                            stopStartMenuItem.setIcon(R.drawable.baseline_stop_black_48);
                            stopStartMenuItem.setTitle(R.string.stop_service);
                        }
                        doBindService();
                        askBatteryOptimizationExclusion();
                        //moveTaskToBack(true);
                        //finish();
                    }
                });
            }
        });

    }

    public void stopTrackingService() {

        /*if( mService != null ) {
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
        }*/
        doUnbindService();
        stopStartMenuItem.setIcon(R.drawable.baseline_not_started_black_48);
        stopStartMenuItem.setTitle(R.string.start_service);
        stopService(new Intent(getBaseContext(), SensorRecorder.class));
        filterAndPublish();
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
            default:
        }
    }

    public void showSettings(MenuItem item) {
        startActivityForResult(new Intent(this, SettingsActivity.class), 0);
    }

    public void showPrivacyAgreement(MenuItem item) {
        startActivity(new Intent(this, PrivacyAgreementActivity.class));
    }

    public void toggleService(MenuItem item) {
        if (isMyServiceRunning(SensorRecorder.class)) {
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

    final Messenger mMessenger = new Messenger(new IncomingMessageHandler());
    Messenger mService = null;
    boolean mIsBound;

    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            mService = new Messenger(service);
            Message msg = Message.obtain(null, SensorRecorder.MSG_REGISTER_CLIENT);
            msg.replyTo = mMessenger;
            try {
                mService.send(msg);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        public void onServiceDisconnected(ComponentName className) {
            mService = null;
        }
    };


    void buildDataPackageFromSensorsJSON(String filename) {
        JSONArray locations = locationService.bgGeo.getLocations();
        Log.d(TAG, "build data package" + filename);
        File file = new File(filename.substring(0, filename.length() - 5) + "_locations.json");
        FileOutputStream fOut = null;
        try {
            fOut = new FileOutputStream(file, true);
            fOut.write(locations.toString().getBytes());
            fOut.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        locationService.bgGeo.destroyLocations();
    }

    private class IncomingMessageHandler extends Handler {
        @Override
        public void handleMessage(@NonNull Message msg) {
            switch (msg.what) {
                case SensorRecorder.MSG_STORED_JSON_FILE:
                    buildDataPackageFromSensorsJSON(msg.obj.toString());
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

    void doBindService() {
        locationService.startBG();
        if (locationService.bgGeo != null) {
            locationService.bgGeo.destroyLocations();
        }
        bindService(new Intent(this,
                SensorRecorder.class), mConnection, Context.BIND_AUTO_CREATE);
        mIsBound = true;
    }

    void doUnbindService() {
        locationService.stopBG();
        if (mIsBound) {
            try {
                Message msg = Message.obtain(null,
                        SensorRecorder.MSG_UNREGISTER_CLIENT);
                msg.replyTo = mMessenger;
                mService.send(msg);
            } catch (RemoteException e) {
                // There is nothing special we need to do if the service
                // has crashed.
            }
            unbindService(mConnection);
            mIsBound = false;
        }
    }

}
