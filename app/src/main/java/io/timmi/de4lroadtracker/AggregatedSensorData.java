package io.timmi.de4lroadtracker;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.os.Build;

import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;


public class AggregatedSensorData {

    private static class AggregatedSensorValues {

        public final List<Float> minVals = new ArrayList<>();
        public final List<Float> maxVals = new ArrayList<>();
        public final List<Float> summedVals = new ArrayList<>();
        public final List<Integer> summedAccuracy = new ArrayList<>();
        public final List<Float> avgVals = new ArrayList<>();
        public final List<Float> avgAccuracy = new ArrayList<>();
        public final List<Integer> numVals = new ArrayList<>();
        @Nullable
        public Float firstTimestamp = null;
        @Nullable
        public Float lastTimestamp = null;
        public Sensor sensor = null;

        public AggregatedSensorValues(Sensor sensor) {
            this.sensor = sensor;
        }

    }

    private final Map<Sensor, AggregatedSensorValues> svMap = new HashMap<>();


    /**
     * Take a Queue of values and process each value to
     * form min, max and average values.
     *
     * The data will be partitioned by the sensor.
     *
     * Each sensor, will get its own min, max , average structure
     *
     * @param sensorValues The queue will be eaten by this class and will be empty after the class has
     *                     been instantiated
     */
    public AggregatedSensorData(Queue<SensorEvent> sensorValues) {
        aggregateSensorData(sensorValues);
    }

    /**
     * you can savely set the nth element of a List, because the List will be
     * expanded with _filler accordingly
     *
     * @param idx     element index to be set
     * @param value  value to be set on idx
     * @param list   the List , that will be mutated
     * @param filler if list is smaller then idx fill the List with _filler
     * @param <T>
     */
    static <T> void setFill(int idx, List<T> list, T value, T filler) {
        while (list.size() <= idx) {
            list.add(filler);
        }
        list.set(idx, value);
    }

    static <T> T getOrElse(int idx, List<T> list, T elseVal) {
        if (list.size() <= idx) {
            return elseVal;
        }
        return list.get(idx);
    }

    private void aggregateSensorData(Queue<SensorEvent> sensorValues) {

        while (!sensorValues.isEmpty()) {
            SensorEvent measurement = sensorValues.remove();
            Sensor sensor = measurement.sensor;

            AggregatedSensorValues sv = new AggregatedSensorValues(sensor);
            if (!svMap.containsKey(sensor)) {
                svMap.put(sensor, new AggregatedSensorValues(sensor));
            } else {
                sv = svMap.get(sensor);
            }
            for (int i = 0; i < measurement.values.length; i++) {
                float val = measurement.values[i];
                // if minimal is not set, it will be -1
                float defaultMin = -1.0f;
                float defaultMax = -1.0f;
                setFill(i, sv.numVals,
                        getOrElse(i, sv.numVals, 0) + 1,
                        0);
                setFill(i, sv.summedVals,
                        getOrElse(i, sv.summedVals, .0f) + val,
                        .0f);
                setFill(i, sv.summedAccuracy,
                        getOrElse(i, sv.summedAccuracy, 0) + measurement.accuracy,
                        0);
                float max = getOrElse(i, sv.maxVals, defaultMax);
                if (val > max || max == defaultMax)
                    setFill(i, sv.maxVals, val, defaultMax);
                float min = getOrElse(i, sv.minVals, defaultMin);
                if (val < min || min == defaultMin)
                    setFill(i, sv.minVals, val, defaultMin);


            }
            for (int i = 0; i < sv.numVals.size(); i++) {
                int countVal = getOrElse(i, sv.numVals, 1);
                setFill(i, sv.avgVals,
                        sv.summedVals.get(i) / countVal,
                        .0f);
                setFill(i, sv.avgAccuracy,
                        ((float) sv.summedAccuracy.get(i)) / countVal,
                        .0f);
            }

            float ts = measurement.timestamp;
            if (sv.firstTimestamp == null || sv.firstTimestamp > ts) {
                sv.firstTimestamp = ts;
            }
            if (sv.lastTimestamp == null || sv.lastTimestamp < ts) {
                sv.lastTimestamp = ts;
            }
        }
    }

    public JSONArray getJSON() {

        JSONArray sensorsValArr = new JSONArray();

        for (Map.Entry<Sensor, AggregatedSensorValues> svEntry : svMap.entrySet()) {

            JSONObject res = new JSONObject();
            Sensor sensor = svEntry.getKey();

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
                valuesJSON.put("minimum", sv.minVals);
                valuesJSON.put("maximum", sv.maxVals);
                valuesJSON.put("average", sv.avgVals);
                valuesJSON.put("averageAccuracy", sv.avgAccuracy);
                valuesJSON.put("countValues", sv.numVals);

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

        return sensorsValArr;
    }
}
