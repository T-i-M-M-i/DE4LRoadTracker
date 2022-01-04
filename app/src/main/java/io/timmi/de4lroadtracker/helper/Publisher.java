package io.timmi.de4lroadtracker.helper;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import org.json.JSONObject;

import java.io.File;

import io.timmi.de4lroadtracker.MQTTConnection;
import io.timmi.de4lroadtracker.SensorRecorder;
import io.timmi.de4lroadtracker.model.ProcessedResult;

public class Publisher {

    private static void storeDistance(double distance, Context context) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        long previousDistance = settings.getLong("distanceMeterTracked", 0);
        SharedPreferences.Editor settingsEditor = settings.edit();
        settingsEditor.putLong("distanceMeterTracked", previousDistance + ((int) Math.round(distance)));
        settingsEditor.apply();
    }

    public static void filterAndPublish(File dir, Context context, MQTTConnection mqttConn) {
        JSONObject deviceInfo = JsonInfoBuilder.getDeviceInfo(context);
        JSONObject appInfo = JsonInfoBuilder.getAppInfo(context);
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        boolean removeFiles = !settings.getBoolean("keepDataOnDevice", false);
        int speedLimit = 5;
        try {
            String speedSettingsString = settings.getString("speedLimitKMH", "5");
            if(speedSettingsString != null)
                speedLimit = Integer.parseInt(speedSettingsString);
        } catch (Exception ignored) { }

        ProcessedResult result = AggregateAndFilter.processResults(dir,
                SensorRecorder.UNPROCESSED_SENSOR_DATA_DIR,
                SensorRecorder.PROCESSED_SENSOR_DATA_DIR ,
                removeFiles, appInfo, deviceInfo, speedLimit);

        if (result == null || result.resultJSON == null) {
            return;
        }

        mqttConn.publishMessage(result.resultJSON);
        storeDistance(result.distance, context);
    }
}
