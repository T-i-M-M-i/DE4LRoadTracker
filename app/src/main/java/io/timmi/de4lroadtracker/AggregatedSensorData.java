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
import java.util.ListIterator;
import java.util.Map;

import io.timmi.de4lroadtracker.model.AggregatedSensorValues;
import io.timmi.de4lroadtracker.model.DE4LSensorEvent;


public class AggregatedSensorData {




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

    /**
     * Take a List of values and process each value to
     * form min, max and average values.
     *
     * The data will be partitioned by type of sensor.
     *
     * Each sensor, will get its own min, max , average structure
     *
     * @param sensorValues The list of values
     */

    public static Map<String, AggregatedSensorValues> aggregateSensorData(List<DE4LSensorEvent> sensorValues) {

        Map<String, AggregatedSensorValues> svMap = new HashMap<>();
        ListIterator<DE4LSensorEvent> valueIterator  = sensorValues.listIterator();

        while (!valueIterator.hasNext()) {
            DE4LSensorEvent measurement = valueIterator.next();
            Sensor sensor = measurement.sensor;

            AggregatedSensorValues svs = new AggregatedSensorValues(measurement.key, sensor);
            if (!svMap.containsKey(svs.key)) {
                svMap.put(svs.key , new AggregatedSensorValues(svs.key, sensor));
            } else {
                svs = svMap.get(svs.key);
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
        for (String key : svMap.keySet()) {
            AggregatedSensorValues svs = svMap.get(key);
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
        return svMap;
    }

    //public JSONArray
}
