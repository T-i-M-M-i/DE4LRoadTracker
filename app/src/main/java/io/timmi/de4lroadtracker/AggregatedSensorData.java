package io.timmi.de4lroadtracker;

import android.hardware.Sensor;
import android.os.SystemClock;

import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import io.timmi.de4lroadtracker.model.AggregatedSensorValues;
import io.timmi.de4lroadtracker.model.DE4LSensorEvent;


public class AggregatedSensorData {

    /**
     * Calculates the static offset (ms) which needs to
     * be added to the `event.time` (ns) to get the Unix
     * timestamp of the event.
     *
     * Credits: https://stackoverflow.com/questions/5500765/accelerometer-sensorevent-timestamp
     *
     * @param eventTimeNanos the {@code SensorEvent.time} to be used to determine the time offset
     * @return the offset in milliseconds
     */
    public static long eventTimeOffset(final long eventTimeNanos) {
        // Capture timestamps of event reporting time
        final long elapsedRealTimeMillis = SystemClock.elapsedRealtime();
        final long upTimeMillis = SystemClock.uptimeMillis();
        final long currentTimeMillis = System.currentTimeMillis();

        // Check which timestamp the event.time is closest to the event.time
        final long eventTimeMillis = eventTimeNanos / 1_000_000L;
        final long elapsedTimeDiff = elapsedRealTimeMillis - eventTimeMillis;
        final long upTimeDiff = upTimeMillis - eventTimeMillis;
        final long currentTimeDiff = currentTimeMillis - eventTimeMillis;

        // Default case (elapsedRealTime, following the documentation)
        if (Math.abs(elapsedTimeDiff) <= Math.min(Math.abs(upTimeDiff), Math.abs(currentTimeDiff))) {
            final long bootTimeMillis = currentTimeMillis - elapsedRealTimeMillis;
            return bootTimeMillis;
        }

        // Other seen case (currentTime, e.g. Nexus 4)
        if (Math.abs(currentTimeDiff) <= Math.abs(upTimeDiff)) {
            return 0;
        }

        // Possible case, but unknown if actually used by manufacturers (upTime)
        throw new IllegalStateException("The event.time seems to be upTime. In this case we cannot use a static offset to calculate the Unix timestamp of the event");
    }

    static long eventTimeNanoToUnixTimeMS(final long eventTimeNanos,final long offsetInMS) {
        return eventTimeNanos / 1_000_000L + offsetInMS;
    }


    /**
     * you can safely set the nth element of a List, because the List will be
     * expanded with _filler accordingly
     *
     * @param idx    element index to be set
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
     * <p>
     * The data will be partitioned by type of sensor.
     * <p>
     * Each sensor, will get its own min, max , average structure
     *
     * @param sensorValues The list of values
     */

    public static Map<String, AggregatedSensorValues> aggregateSensorData(List<DE4LSensorEvent> sensorValues) {

        Map<String, AggregatedSensorValues> svMap = new HashMap<>();
        ListIterator<DE4LSensorEvent> valueIterator = sensorValues.listIterator();
        Long offsetInMS = null;

        while (valueIterator.hasNext()) {
            DE4LSensorEvent measurement = valueIterator.next();
            Sensor sensor = measurement.sensor;
            if(offsetInMS == null) {
                offsetInMS = eventTimeOffset(measurement.timestamp);
            }

            AggregatedSensorValues svs = new AggregatedSensorValues(measurement.key, sensor);
            if (!svMap.containsKey(svs.key)) {
                svMap.put(svs.key, new AggregatedSensorValues(svs.key, sensor));
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
                if (val > max)
                    setFill(i, svs.maxVals, val, Float.NEGATIVE_INFINITY);

                float min = getOrElse(i, svs.minVals, Float.POSITIVE_INFINITY);
                if (val < min)
                    setFill(i, svs.minVals, val, Float.POSITIVE_INFINITY);


            }

            long ts = measurement.timestamp;
            if (svs.firstTimestamp == null || svs.firstTimestamp > ts) {
                svs.firstTimestamp = ts;
                svs.firstUnixTimestampInMS = eventTimeNanoToUnixTimeMS(ts, offsetInMS);
            }
            if (svs.lastTimestamp == null || svs.lastTimestamp < ts) {
                svs.lastTimestamp = ts;
                svs.lastUnixTimestampInMS = eventTimeNanoToUnixTimeMS(ts, offsetInMS);
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
                        Math.sqrt(svs.summedQuadVals.get(i) / countVal),
                        .0d);
            }
        }
        return svMap;
    }

    //public JSONArray
}
