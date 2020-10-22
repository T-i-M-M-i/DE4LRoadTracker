package io.timmi.de4lroadtracker;

import android.annotation.SuppressLint;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.provider.Settings.Secure;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

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

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

public class SensorRecordService extends Service implements SensorEventListener {
    private static String TAG = "DE4SensorRecordService";
    private static final int NOTIFICATION = 1;
    public static final String CLOSE_ACTION = "close";
    @Nullable
    private NotificationManager mNotificationManager = null;
    private final NotificationCompat.Builder mNotificationBuilder = new NotificationCompat.Builder(this);
    @Nullable
    private SensorManager sensorManager = null;
    @Nullable
    private Sensor accelerometer = null;
    @Nullable
    private Sensor light = null;
    @Nullable
    private SensorEvent lastLightSensorEvent = null;
    @Nullable
    private SensorEvent lastAccSensorEvent = null;
    @Nullable
    private BackgroundGeolocation bgGeo = null;
    @Nullable
    private MQTTConnection mqttConnection = null;
    @Nullable
    private JSONObject deviceInfo = getDeviceJSON(null);
    private Queue<SensorEvent> sensorEventQueue = new LinkedList<>();
    /**
     * How many sensor events to store in the Queue, before dropping
     */
    private final int maxQueueSize = 10000;
    private final Queue<SensorEvent> lightSensorQueue = new LinkedBlockingQueue<SensorEvent>();
    private final Queue<SensorEvent> accSensorQueue = new LinkedBlockingQueue<SensorEvent>();



    @SuppressLint("HardwareIds")
    private JSONObject getDeviceJSON(Context _ctx) {
        JSONObject _deviceInfo = new JSONObject();
        try {
            _deviceInfo.put("android_id",
                    _ctx == null
                            ? "not yet available"
                            : Secure.getString(_ctx.getContentResolver(), Secure.ANDROID_ID));
            _deviceInfo.put("manufacturer", Build.MANUFACTURER);
            _deviceInfo.put("model", Build.MODEL);
            _deviceInfo.put("device", Build.DEVICE);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return _deviceInfo;

    }

    private void setupSensors() {
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        light = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        sensorManager.registerListener(this, accelerometer , SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, light , SensorManager.SENSOR_DELAY_NORMAL);
    }


    public JSONObject sensorValueToJson(SensorEvent sensorValue) {
        JSONObject  res = new JSONObject();
        if(sensorValue == null)
            return res;
        try {
            res.put("values", new JSONArray(sensorValue.values));
            res.put("name", sensorValue.sensor.getName());
            res.put("type", sensorValue.sensor.getType());
            res.put("accuracy", sensorValue.accuracy);
            res.put("timestamp", sensorValue.timestamp);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return res;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    public void onCreate() {
        super.onCreate();
        Toast.makeText(getBaseContext(), "tracking service started", Toast.LENGTH_SHORT).show();

        deviceInfo = getDeviceJSON(getApplicationContext());

        setupNotifications();
        showNotification();
        setupSensors();

        mqttConnection = new MQTTConnection(this, getApplicationContext());

        // Get a reference to the SDK
        bgGeo = BackgroundGeolocation.getInstance(getApplicationContext());
        //final TSConfig config = TSConfig.getInstance(getApplicationContext());

        // Configure the SDK
        /*config.updateWithBuilder()
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
*/
        // Listen events
        bgGeo.onLocation(new TSLocationCallback() {
            @Override
            public void onLocation(TSLocation location) {
                Log.i(TAG, "[location] from service" + location.toJson());
                Log.i(TAG, "[sensorEventQueue] size: " + sensorEventQueue.size());
                final JSONArray allSensorsData  = new AggregatedSensorData(sensorEventQueue).getJSON();
                JSONObject geoPoint = new JSONObject();
                JSONObject publishLocMessage = new JSONObject();
                try {
                    geoPoint.put("lat", location.getLocation().getLatitude());
                    geoPoint.put("lon", location.getLocation().getLongitude());

                        publishLocMessage.put("deviceInfo", deviceInfo);

                    publishLocMessage.put("location", location.toJson());
                    publishLocMessage.put("data", allSensorsData);
                    //publishLocMessage.put("acceleration", sensorValueToJson(lastAccSensorEvent));
                    //publishLocMessage.put("light", sensorValueToJson(lastLightSensorEvent));
                    publishLocMessage.put("geoPoint", geoPoint);
                    mqttConnection.publishMessage(publishLocMessage.toString(2));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            @Override
            public void onError(Integer code) {
                Log.i(TAG, "[location] ERROR: " + code);
            }
        });

        // Finally, signal #ready to the SDK.
        /*bgGeo.ready(new TSCallback() {
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
        });*/
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Toast.makeText(getBaseContext(),"tracking service stopped", Toast.LENGTH_LONG).show();
        if (mNotificationManager != null) {
            mNotificationManager.cancel(NOTIFICATION);
        }
        if (bgGeo != null) {
            Log.d(TAG, "Will stop the location service");
            bgGeo.stop();
            //bgGeo.getCount()
        }
    }

    private void setupNotifications() { //called in onCreate()
        if (mNotificationManager == null) {
            mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        }
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, MainActivity.class)
                        .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP),
                0);
        PendingIntent pendingCloseIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, MainActivity.class)
                        .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP)
                        .setAction(CLOSE_ACTION),
                0);
        mNotificationBuilder
                .setSmallIcon(R.drawable.ic_launcher_background)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setContentTitle(getText(R.string.app_name))
                .setWhen(System.currentTimeMillis())
                .setContentIntent(pendingIntent)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel,
                        getString(R.string.action_exit), pendingCloseIntent)
                .setOngoing(true);
    }

    private void showNotification() {
        mNotificationBuilder
                .setTicker(getText(R.string.service_connected))
                .setContentText(getText(R.string.service_connected));
        if (mNotificationManager != null) {
            mNotificationManager.notify(NOTIFICATION, mNotificationBuilder.build());
        }
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
       // Log.i(TAG, "[sensorChanged] " + sensorEvent.toString());
        switch (sensorEvent.sensor.getType()) {
            case Sensor.TYPE_LIGHT: {
                lastLightSensorEvent = sensorEvent;
                sensorEventQueue.add(sensorEvent);
                break;
            }
            case Sensor.TYPE_ACCELEROMETER: {
                lastAccSensorEvent = sensorEvent;
                sensorEventQueue.add(sensorEvent);
                break;
            }
        }
    }


    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
        //Log.i(TAG, "[accuracyChanged] " + sensor.getName() + " " + i);

    }
}
