package io.timmi.de4lroadtracker;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings.Secure;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.transistorsoft.locationmanager.adapter.BackgroundGeolocation;
import com.transistorsoft.locationmanager.adapter.callback.TSLocationCallback;
import com.transistorsoft.locationmanager.location.TSLocation;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

import io.timmi.de4lroadtracker.model.DE4LSensorEvent;

public class SensorRecordService extends Service implements SensorEventListener {
    private static String TAG = "DE4SensorRecordService";
    private static final int NOTIFICATION = 1;
    public static final String CLOSE_ACTION = "close";
    @Nullable
    private NotificationManager mNotificationManager = null;
    private final NotificationCompat.Builder mNotificationBuilder = new NotificationCompat.Builder(this);
    @Nullable
    private BackgroundGeolocation bgGeo = null;
    @Nullable
    private MQTTConnection mqttConnection = null;
    @Nullable
    private JSONObject deviceInfo = getDeviceJSON(null);
    private final Queue<DE4LSensorEvent> sensorEventQueue = new LinkedList<>();
    @Nullable
    private JSONObject appInfo = null;
    /**
     * How many sensor events to store in the Queue, before dropping
     */
    private final int maxQueueSize = 10000;


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
        SensorManager sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        Sensor accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        Sensor light = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        Sensor magneticField = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        Sensor linearAcceleration = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        Sensor rotationVector = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);

        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, light, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, magneticField, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, linearAcceleration, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, rotationVector, SensorManager.SENSOR_DELAY_NORMAL);


    }


    public JSONObject sensorValueToJson(SensorEvent sensorValue) {
        JSONObject res = new JSONObject();
        if (sensorValue == null)
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

    private JSONObject getAppInfo() {
        JSONObject res = new JSONObject();
        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            res.put("version", pInfo.versionName);
            res.put("versionCode", pInfo.versionCode);
            res.put("packageName", pInfo.packageName);
        } catch (PackageManager.NameNotFoundException | JSONException e) {
            e.printStackTrace();
        }
        return res;

    }

    public void onCreate() {
        super.onCreate();
        Toast.makeText(getBaseContext(), "tracking service started", Toast.LENGTH_SHORT).show();

        deviceInfo = getDeviceJSON(getApplicationContext());

        appInfo = getAppInfo();
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
                final JSONArray allSensorsData = new AggregatedSensorData(sensorEventQueue).getJSON();
                JSONObject geoPoint = new JSONObject();
                JSONObject publishLocMessage = new JSONObject();
                try {
                    geoPoint.put("lat", location.getLocation().getLatitude());
                    geoPoint.put("lon", location.getLocation().getLongitude());


                    publishLocMessage.put("deviceInfo", deviceInfo);
                    publishLocMessage.put("appInfo", appInfo);

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

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Toast.makeText(getBaseContext(), "tracking service stopped", Toast.LENGTH_LONG).show();
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

    private void addToSensorEventQueue(SensorEvent sensorEvent, boolean shouldLog) {
        sensorEventQueue.add(new DE4LSensorEvent(sensorEvent));
        if (!shouldLog) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < sensorEvent.values.length; i++) {
            sb.append(sensorEvent.values[i]).append(" ");
        }
        Log.d(TAG, sensorEvent.timestamp + " value: " + sb.toString());
    }

    private  DE4LSensorEvent accelerometerReading = new DE4LSensorEvent(3);
    private  DE4LSensorEvent magnetometerReading = new DE4LSensorEvent(3);

    private final float[] rotationMatrix = new float[9];
    private final float[] orientationAngles = new float[3];
    private final float[] rotationInverted = new float[16];



    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        // Log.i(TAG, "[sensorChanged] " + sensorEvent.toString());
        switch (sensorEvent.sensor.getType()) {
            case Sensor.TYPE_LIGHT: {
                addToSensorEventQueue(sensorEvent, false);
                break;
            }
            case Sensor.TYPE_ACCELEROMETER: {
                //addToSensorEventQueue(sensorEvent, true);
                accelerometerReading = new DE4LSensorEvent(sensorEvent);
                break;
            }
            case Sensor.TYPE_MAGNETIC_FIELD: {
                orientationAngles = new DE4LSensorEvent(sensorEvent);
                break;
            }
            case Sensor.TYPE_ROTATION_VECTOR: {
                float[] rotation = new float[16]
                SensorManager.getRotationMatrixFromVector(rotation, sensorEvent.values);
                android.opengl.Matrix.invertM(rotationInverted, 0, rotation, 0);
                break;

            }
            case Sensor.TYPE_LINEAR_ACCELERATION: {
                float[] linAcc = new float[sensorEvent.values.length];
                float[] accelartionAxis = new float[sensorEvent.values.length];
                System.arraycopy(sensorEvent.values, 0, linAcc, 0, sensorEvent.values.length);
                android.opengl.Matrix.multiplyMV(accelartionAxis, 0, rotationInverted, 0, linAcc, 0);
                sensorEventQueue.add(new DE4LSensorEvent(sensorEvent.accuracy, sensorEvent.sensor, sensorEvent.timestamp, linAcc ))
                break;
            }
        }
    }

    // Compute the three orientation angles based on the most recent readings from
    // the device's accelerometer and magnetometer.
    public void updateOrientationAngles() {
        // Update rotation matrix, which is needed to update orientation angles.
        SensorManager.getRotationMatrix(rotationMatrix, null,
                accelerometerReading.values, magnetometerReading.values);

        // "rotationMatrix" now has up-to-date information.
        SensorManager.getOrientation(rotationMatrix, orientationAngles);
        // "orientationAngles" now has up-to-date information.
    }



    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
        //Log.i(TAG, "[accuracyChanged] " + sensor.getName() + " " + i);

    }
}
