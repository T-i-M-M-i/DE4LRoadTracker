package io.timmi.de4lroadtracker;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import io.timmi.de4lroadtracker.helper.AggregateAndFilter;
import io.timmi.de4lroadtracker.helper.JsonInfoBuilder;
import io.timmi.de4lroadtracker.helper.JsonSerializer;
import io.timmi.de4lroadtracker.helper.TrackerIndicatorNotification;
import io.timmi.de4lroadtracker.model.AggregatedSensorValues;
import io.timmi.de4lroadtracker.model.DE4LSensorEvent;

public class SensorRecorder extends Service implements SensorEventListener {

    final static int MSG_STORED_JSON_FILE = 10;
    final static int MSG_REGISTER_CLIENT = 20;
    final static int MSG_UNREGISTER_CLIENT = 30;

    private final static String TAG = "DE4SensorRecordService";
    private final static Integer STORE_QUEUE_SIZE = 500;
    private final static Integer PUBLISH_AFTER_STORE_SIZE = 3;

    public final static String UNPROCESSED_SENSOR_DATA_DIR = "unprocessed";
    public final static String PROCESSED_SENSOR_DATA_DIR = "processed";

    private SharedPreferences settings;

    private Integer unpublishedStoreCount = 0;

    private List<DE4LSensorEvent> sensorEventQueue = new LinkedList<>();

    private TrackerIndicatorNotification notification = new TrackerIndicatorNotification(this);

    @Nullable
    private MQTTConnection mqttConnection = null;

    private MQTTConnection getMqttConnection() {
        if (mqttConnection == null) {
            mqttConnection = new MQTTConnection(getApplicationContext(), getApplicationContext());
        }
        return mqttConnection;
    }

    private void clearSensorData() {
        sensorEventQueue = new LinkedList<DE4LSensorEvent>();
    }

    public SensorRecorder() {
    }

    public void onCreate() {
        super.onCreate();
        Toast.makeText(getBaseContext(), "tracking service started", Toast.LENGTH_SHORT).show();

        notification.setupNotifications();
        notification.showNotification();
        settings = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        setupSensors();
        getMqttConnection();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Toast.makeText(getBaseContext(), "tracking service stopped", Toast.LENGTH_LONG).show();
        notification.closeNotification();
    }

    private void setupSensors() {
        SensorManager sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        Sensor light = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        Sensor linearAcceleration = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        Sensor rotationVector = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);

        Sensor[] sensors = { light, linearAcceleration, rotationVector };

        storeMetaData(sensors);

        sensorManager.registerListener(this, light, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, linearAcceleration, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, rotationVector, SensorManager.SENSOR_DELAY_NORMAL);
    }

    ArrayList<Messenger> mClients = new ArrayList<Messenger>();


    private class MessageHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_REGISTER_CLIENT:
                    mClients.add(msg.replyTo);
                    break;
                case MSG_UNREGISTER_CLIENT:
                    mClients.remove(msg.replyTo);
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

    private final Messenger mMessenger = new Messenger(new SensorRecorder.MessageHandler());

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        //throw new UnsupportedOperationException("Not yet implemented");
        return mMessenger.getBinder();
    }

    private final float[] rotationInverted = new float[16];

    final private String fileName = "sensor_data.json";

    void sendStoredJsonFileEvent(String fileName) {
        Message storedDataMessage = Message.obtain(null, SensorRecorder.MSG_STORED_JSON_FILE, fileName);
        for (int i = mClients.size() - 1; i >= 0; i--) {
            try {
                mClients.get(i).send(storedDataMessage);
            } catch (RemoteException e) {
                // The client is dead.  Remove it from the list;
                // we are going through the list from back to front
                // so this is safe to do inside the loop.
                mClients.remove(i);
            }
        }
    }

    private void storeMetaData(Sensor[] sensors) {
        JSONArray sensorInfos = new JSONArray();
        for (Sensor sensor :
                sensors) {
            try {

                JSONObject sInfo = JsonSerializer.buildSensorInfoJSON(sensor);
                sensorInfos.put(sInfo);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        try {
            File file = new File(getExternalFilesDir(null), "sensors_info.json");
            FileOutputStream fOut = new FileOutputStream(file, false);
            fOut.write(sensorInfos.toString().getBytes());
            fOut.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean storeData() {
        //Map<String, AggregatedSensorValues> data = AggregatedSensorData.aggregateSensorData(sensorEventQueue);
        try {
            JSONObject sensorDataJSON = JsonSerializer.groupSensorDataToJSON(sensorEventQueue);

            File directory = new File(getExternalFilesDir(null)+File.separator+UNPROCESSED_SENSOR_DATA_DIR);
            if(!directory.exists()) directory.mkdirs();


            File file = File.createTempFile(fileName, ".json", directory);
            FileOutputStream fOut = new FileOutputStream(file, true);
            //FileOutputStream fOut = openFileOutput(fileName, MODE_APPEND);
            fOut.write(sensorDataJSON.toString().getBytes());
            fOut.close();
            clearSensorData();
            sendStoredJsonFileEvent(file.getAbsolutePath());
            return true;
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

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
        if (sensorEventQueue.size() > STORE_QUEUE_SIZE) {
            boolean success = storeData();
            if(success) {
                unpublishedStoreCount++;
            }
        }
        if (unpublishedStoreCount >= PUBLISH_AFTER_STORE_SIZE) {
            unpublishedStoreCount = 0;
            filterAndPublish();
        }
    }

    private void filterAndPublish() {
        File dir = getExternalFilesDir(null);
        JSONObject deviceInfo = JsonInfoBuilder.getDeviceInfo(getApplicationContext());
        JSONObject appInfo = JsonInfoBuilder.getAppInfo(this);
        boolean removeFiles = !settings.getBoolean("keepDataOnDevice", false);
        int speedLimit = Integer.parseInt(settings.getString("speedLimitKMH", "5"));
        String resultJson = AggregateAndFilter.processResults(
                dir,
                SensorRecorder.UNPROCESSED_SENSOR_DATA_DIR,
                SensorRecorder.PROCESSED_SENSOR_DATA_DIR ,
                removeFiles,
                appInfo,
                deviceInfo,
                speedLimit);
        if (resultJson == null) {
            return;
        }
        getMqttConnection().publishMessage(resultJson);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
    }

}
