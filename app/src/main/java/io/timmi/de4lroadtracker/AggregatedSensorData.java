package io.timmi.de4lroadtracker;

import android.hardware.Sensor;
import android.os.Build;
import android.util.Log;

import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import io.timmi.de4lroadtracker.model.DE4LSensorEvent;


public class AggregatedSensorData {

    private static class AggregatedSensorValues {

        public final List<Float> minVals = new ArrayList<>();
        public final List<Float> maxVals = new ArrayList<>();
        public final List<Float> summedVals = new ArrayList<>();
        public final List<Integer> summedAccuracy = new ArrayList<>();
        public final List<Double> summedQuadVals = new ArrayList<>();
        public final List<Float> avgVals = new ArrayList<>();
        public final List<Float> avgAccuracy = new ArrayList<>();
        public final List<Integer> numVals = new ArrayList<>();
        @Nullable
        public Float firstTimestamp = null;
        @Nullable
        public Float lastTimestamp = null;
        public Sensor sensor;

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
    public AggregatedSensorData(Queue<DE4LSensorEvent> sensorValues) {
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

    private void aggregateSensorData(Queue<DE4LSensorEvent> sensorValues) {


        while (!sensorValues.isEmpty()) {
            DE4LSensorEvent measurement = sensorValues.remove();
            Sensor sensor = measurement.sensor;

            AggregatedSensorValues svs = new AggregatedSensorValues(sensor);
            if (!svMap.containsKey(sensor)) {
                svMap.put(sensor, new AggregatedSensorValues(sensor));
            } else {
                svs = svMap.get(sensor);
            }


            for (int i = 0; i < measurement.values.length; i++) {
                float val = measurement.values[i];
                double quadVal = (double) val * (double) val;

                setFill(i, svs.summedQuadVals,
                        getOrElse(i, svs.summedQuadVals, .0d) + quadVal,
                        .0d);
                setFill(i, svs.numVals,
                        getOrElse(i, svs.numVals, 0) + 1,
                        0);
                setFill(i, svs.summedVals,
                        getOrElse(i, svs.summedVals, .0f) + val,
                        .0f);
                setFill(i, svs.summedAccuracy,
                        getOrElse(i, svs.summedAccuracy, 0) + measurement.accuracy,
                        0);

                float max = getOrElse(i, svs.maxVals, Float.NEGATIVE_INFINITY);
                if ( val > max)
                    setFill(i, svs.maxVals, val, Float.NEGATIVE_INFINITY);

                float min = getOrElse(i, svs.minVals, Float.POSITIVE_INFINITY);
                if (val < min)
                    setFill(i, svs.minVals, val, Float.POSITIVE_INFINITY);


            }

            float ts = measurement.timestamp;
            if (svs.firstTimestamp == null || svs.firstTimestamp > ts) {
                svs.firstTimestamp = ts;
            }
            if (svs.lastTimestamp == null || svs.lastTimestamp < ts) {
                svs.lastTimestamp = ts;
            }
        }
        for (Sensor sensor : svMap.keySet()) {
            AggregatedSensorValues svs = svMap.get(sensor);
            for (int i = 0; i < svs.numVals.size(); i++) {
                int countVal = getOrElse(i, svs.numVals, 1);
                setFill(i, svs.avgVals,
                        svs.summedVals.get(i) / countVal,
                        .0f);
                setFill(i, svs.avgAccuracy,
                        ((float) svs.summedAccuracy.get(i)) / countVal,
                        .0f);
                setFill(i, svs.summedQuadVals,
                        Math.sqrt( (double) svs.summedQuadVals.get(i) / countVal),
                        .0d);
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
