package io.timmi.de4lroadtracker.helper;

import android.hardware.Sensor;
import android.os.Build;
import android.util.Log;

import com.transistorsoft.locationmanager.location.TSLocation;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

import io.timmi.de4lroadtracker.model.AggregatedSensorValues;

public class JsonSerializer {


    public static  JSONArray aggregatedSensorDataToJSON(Map<String, AggregatedSensorValues> svMap) {

        JSONArray sensorsValArr = new JSONArray();

        for (Map.Entry<String, AggregatedSensorValues> svEntry : svMap.entrySet()) {

            JSONObject res = new JSONObject();
            Sensor sensor = svEntry.getValue().sensor;

            JSONObject sensorJSON = new JSONObject();

            try {
                sensorJSON.put("name", sensor.getName());
                sensorJSON.put("type", sensor.getType());
                sensorJSON.put("vendor", sensor.getVendor());
                sensorJSON.put("maximumRange", sensor.getMaximumRange());
                sensorJSON.put("power", sensor.getPower());
                sensorJSON.put("resolution", sensor.getResolution());
                sensorJSON.put("version", sensor.getVersion());
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    sensorJSON.put("id", sensor.getId());
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    sensorJSON.put("highestDirectReportRateLevel", sensor.getHighestDirectReportRateLevel());
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

            AggregatedSensorValues sv = svEntry.getValue();

            JSONObject valuesJSON = new JSONObject();
            try {
                valuesJSON.put("minimum", new JSONArray(sv.minVals));
                valuesJSON.put("maximum", new JSONArray(sv.maxVals));
                //valuesJSON.put("standardDeviation", new JSONArray(sv.summedQuadVals));
                valuesJSON.put("average", new JSONArray(sv.avgVals));
                valuesJSON.put("averageAccuracy", new JSONArray(sv.avgAccuracy));
                valuesJSON.put("countValues", new JSONArray(sv.numVals));

                if (sv.firstTimestamp != null) {
                    valuesJSON.put("firstTimestamp", sv.firstTimestamp);
                }
                if (sv.lastTimestamp != null) {
                    valuesJSON.put("lastTimestamp", sv.lastTimestamp);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            try {
                res.put("sensor", sensorJSON);
                res.put("aggregatedValues", valuesJSON);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            sensorsValArr.put(res);
        }
        Log.d("AggregatedSensorValue", "arr: " + sensorsValArr);

        return sensorsValArr;
    }

}
