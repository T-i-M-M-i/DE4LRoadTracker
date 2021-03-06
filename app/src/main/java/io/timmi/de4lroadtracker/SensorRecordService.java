package io.timmi.de4lroadtracker;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.transistorsoft.locationmanager.adapter.BackgroundGeolocation;
import com.transistorsoft.locationmanager.adapter.callback.TSLocationCallback;
import com.transistorsoft.locationmanager.location.TSLocation;

import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.LinkedList;
import java.util.List;

import io.timmi.de4lroadtracker.helper.DebugChannel;
import io.timmi.de4lroadtracker.helper.JsonInfoBuilder;
import io.timmi.de4lroadtracker.helper.JsonSerializer;
import io.timmi.de4lroadtracker.helper.TSLocationWrapper;
import io.timmi.de4lroadtracker.helper.TrackerIndicatorNotification;
import io.timmi.de4lroadtracker.model.DE4LSensorEvent;

public class SensorRecordService extends Service implements SensorEventListener {

    public final static int STOP_SERVICE_MESSAGE = 1;

    private static String TAG = "DE4SensorRecordService";
    @Nullable
    private MQTTConnection mqttConnection = null;
    private List<DE4LSensorEvent> sensorEventQueue = new LinkedList<>();
    @Nullable
    private JSONObject appInfo = null;
    @Nullable
    private JSONObject deviceInfo = null;
    private TrackerIndicatorNotification notification = new TrackerIndicatorNotification(this);
    @Nullable
    private Location previousLocation = null;

    @Nullable
    private TSLocationWrapper locationService = null;

    private class MessageHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            Log.d(TAG, "received message " + msg.what);
            if(msg.what == STOP_SERVICE_MESSAGE) {
                if(mqttConnection != null) {
                    try {
                        mqttConnection.disconnect(new IMqttActionListener() {
                            @Override
                            public void onSuccess(IMqttToken asyncActionToken) {
                                Log.d(TAG, "disconnect success");
                                DebugChannel.sendHistoryBroadcast(getApplicationContext() , TAG, "mqtt-Connection disconnected");
                                mqttConnection.close();
                                stopSelf();
                            }

                            @Override
                            public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                                Log.d(TAG, "disconnect failure");
                                DebugChannel.sendHistoryBroadcast(getApplicationContext() , TAG, "failure while disconnection mqtt-Connection");
                                stopSelf();
                            }
                        });
                    } catch (MqttException e) {
                        e.printStackTrace();
                        DebugChannel.sendHistoryBroadcast(getApplicationContext() , TAG, "failure while disconnection mqtt-Connection");
                        stopSelf();
                    }
                } else {
                    stopSelf();
                }
            }
        }
    }

    private final Messenger mMessenger = new Messenger(new MessageHandler());

    /**
     * When binding to the service, we return an interface to our messenger
     * for sending messages to the service.
     */
    @Override
    public IBinder onBind(Intent intent) {
        return mMessenger.getBinder();
    }



    private void clearSensorData() {
        sensorEventQueue = new LinkedList<DE4LSensorEvent>();
    }

    private void setupSensors() {
        SensorManager sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        Sensor light = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        Sensor linearAcceleration = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        Sensor rotationVector = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);

        sensorManager.registerListener(this, light, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, linearAcceleration, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, rotationVector, SensorManager.SENSOR_DELAY_NORMAL);
    }


    private void storeDistance(float distance) {

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        Long previousDistance = settings.getLong("distanceMeterTracked", 0);
        SharedPreferences.Editor settingsEditor = settings.edit();
        settingsEditor.putLong("distanceMeterTracked", previousDistance + ((int) Math.round(distance)));
        settingsEditor.apply();
    }

    public void onCreate() {
        super.onCreate();
        Toast.makeText(getBaseContext(), "tracking service started", Toast.LENGTH_SHORT).show();

        notification.setupNotifications();
        notification.showNotification();

        deviceInfo = JsonInfoBuilder.getDeviceInfo(getApplicationContext());
        appInfo = JsonInfoBuilder.getAppInfo(this);

        setupSensors();

        mqttConnection = new MQTTConnection(this, getApplicationContext());
        locationService = new TSLocationWrapper(getApplicationContext());

        // Get a reference to the SDK
        locationService.bgGeo.onLocation(new TSLocationCallback() {
            @Override
            public void onLocation(TSLocation location) {
                if (previousLocation != null) {
                    float distance = previousLocation.distanceTo(location.getLocation());
                    storeDistance(distance);
                }
                previousLocation = new Location(location.getLocation());
                Log.d(TAG, "[location] from service" + location.toJson());
                Log.d(TAG, "[sensorEventQueue] size: " + sensorEventQueue.size());


                try {
                    JSONObject finalMessageJSON = JsonSerializer.finalMessageDataToJSON(
                            location,
                            AggregatedSensorData.aggregateSensorData(sensorEventQueue),
                            deviceInfo,
                            appInfo);
                    mqttConnection.publishMessage(finalMessageJSON.toString(2));
                } catch (JSONException e) {
                    e.printStackTrace();
                } finally {
                    clearSensorData();
                }
            }

            @Override
            public void onError(Integer code) {
                Log.i(TAG, "[location] ERROR: " + code);
            }
        });
        locationService.startBG();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Toast.makeText(getBaseContext(), "tracking service stopped", Toast.LENGTH_LONG).show();
        notification.closeNotification();
        //if (mqttConnection != null) {
            //mqttConnection.close();
        //}
        if (locationService != null) {
            Log.d(TAG, "Will stop the location service");
            locationService.stopBG();
            //bgGeo.getCount()
        }
    }


    private final float[] rotationInverted = new float[16];

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        switch (sensorEvent.sensor.getType()) {
            case Sensor.TYPE_LIGHT: {
                sensorEventQueue.add(new DE4LSensorEvent("light", sensorEvent));
                break;
            }
            case Sensor.TYPE_ROTATION_VECTOR: {
                float[] rotation = new float[16];
                SensorManager.getRotationMatrixFromVector(rotation, sensorEvent.values);
                android.opengl.Matrix.invertM(rotationInverted, 0, rotation, 0);
                break;
            }
            case Sensor.TYPE_LINEAR_ACCELERATION: {
                float[] linAcc = new float[4];
                float[] accelerationAxis = new float[4];

                System.arraycopy(sensorEvent.values, 0, linAcc, 0, sensorEvent.values.length);
                android.opengl.Matrix.multiplyMV(accelerationAxis, 0, rotationInverted, 0, linAcc, 0);
                sensorEventQueue.add(new DE4LSensorEvent("acceleration", sensorEvent.accuracy, sensorEvent.sensor, sensorEvent.timestamp, accelerationAxis));
                break;
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
    }

}
