package io.timmi.de4lroadtracker.helper;

import android.hardware.Sensor;
import android.os.Build;

import com.transistorsoft.locationmanager.location.TSLocation;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;
import java.util.Map;

import io.timmi.de4lroadtracker.model.AggregatedSensorValues;

public class JsonSerializer {


    /**
     * increase PATCH if slight changes, bug fixes of the current schema have been done
     * increase minor only if properties have been added, or new data will be delivered
     * increase MAJOR, if the schema a diverged in an incompatible manner, or important properties have been removed
     */
    static final String SCHEMA_VERSION = "2.1.1";

    /**
     * will merge keys of all objs , if same keys occur in multiple objects the
     * later will overwrite the earlier
     * like {...obj[0], ...obj[1], ...obj[n]}
     * @param objs
     * @return merged object
     */
    public static JSONObject mergeJSONObjects(JSONObject[] objs) {
        JSONObject res = new JSONObject();
        for (JSONObject o :
                objs) {
            for (Iterator<String> it = o.keys(); it.hasNext(); ) {
                String key = it.next();
                try {
                    res.put(key, o.get(key));
                } catch (JSONException ignored) { }
            }
        }
        return res;
    }

    public static JSONObject buildGeneralSensorJSON(AggregatedSensorValues sv) throws JSONException {
        Sensor sensor = sv.sensor;

        JSONObject valuesJSON = new JSONObject();
        JSONObject sensorJSON = new JSONObject();

        valuesJSON.put("vendor", sensor.getVendor());
        valuesJSON.put("name", sensor.getName());
        sensorJSON.put("type", sensor.getType());
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
        valuesJSON.put("additionalProperties", sensorJSON);

        if (sv.firstTimestamp != null) {
            valuesJSON.put("firstTimestamp", sv.firstTimestamp);
        }
        if (sv.lastTimestamp != null) {
            valuesJSON.put("lastTimestamp", sv.lastTimestamp);
        }
        valuesJSON.put("numberOfAggregatedValues", sv.summedVals);

        return valuesJSON;
    }


    public static JSONObject buildMinMaxAvgJSON(int index, AggregatedSensorValues sv) throws JSONException {
        JSONObject res = new JSONObject();
        if(sv.minVals.size() <= index) res.put("minimum", sv.minVals.get(index));
        if(sv.maxVals.size() <= index) res.put("maximum", sv.maxVals.get(index));
        if(sv.avgVals.size() <= index) res.put("average", sv.avgVals.get(index));
        return res;
    }

    public static JSONObject aggregatedSensorDataToJSON(Map<String, AggregatedSensorValues> svMap) throws JSONException {

        JSONObject res = new JSONObject();

        if(svMap.containsKey("acceleration")) {
            AggregatedSensorValues sv = svMap.get("acceleration");
            JSONObject valuesJSON = buildGeneralSensorJSON(sv);
            valuesJSON.put("xAxis", buildMinMaxAvgJSON(0, sv));
            valuesJSON.put("yAxis", buildMinMaxAvgJSON(1, sv));
            valuesJSON.put("zAxis", buildMinMaxAvgJSON(2, sv));
            res.put("accelaration", valuesJSON);
        }
        if(svMap.containsKey("light")) {
            AggregatedSensorValues sv = svMap.get("light");
            JSONObject valuesJSON = buildGeneralSensorJSON(sv);
            valuesJSON.put("lux", buildMinMaxAvgJSON(0, sv));
            res.put("light", valuesJSON);
        }

        return res;
    }


    public static JSONObject finalMessageDataToJSON(TSLocation location, Map<String, AggregatedSensorValues> aggregatedSensorValuesMap, JSONObject deviceInfo, JSONObject appInfo) throws JSONException {
        JSONObject resJSON = new JSONObject();
        JSONObject geoPoint = new JSONObject();
        geoPoint.put("lat", location.getLocation().getLatitude());
        geoPoint.put("lon", location.getLocation().getLongitude());
        resJSON.put("geoPoint", geoPoint);

        JSONObject detectedActivityJSON = new JSONObject();
        detectedActivityJSON.put("confidence", location.getDetectedActivity().getConfidence());
        detectedActivityJSON.put("type", location.getDetectedActivity().getType());
        resJSON.put("detectedActivity", detectedActivityJSON);

        JSONObject coordsJSON = new JSONObject();
        coordsJSON.put("altitude", location.getLocation().getAltitude());
        coordsJSON.put("bearing", location.getLocation().getBearing());
        coordsJSON.put("accuracy", location.getLocation().getAccuracy());
        coordsJSON.put("speed", location.getLocation().getSpeed());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            coordsJSON.put("speedAccuracyMetersPerSecond", location.getLocation().getSpeedAccuracyMetersPerSecond());
        }
        resJSON.put("coords", mergeJSONObjects(new JSONObject[] { coordsJSON, geoPoint }));

        resJSON.put("deviceInfo", deviceInfo);
        resJSON.put("appInfo", appInfo);

        resJSON.put("timestamp", location.getTimestamp());
        resJSON.put("schemaVersion", SCHEMA_VERSION);

        JSONObject sensorDataJSON = JsonSerializer.aggregatedSensorDataToJSON(aggregatedSensorValuesMap);
        return mergeJSONObjects(new JSONObject[] { resJSON, sensorDataJSON  });
    }

}
