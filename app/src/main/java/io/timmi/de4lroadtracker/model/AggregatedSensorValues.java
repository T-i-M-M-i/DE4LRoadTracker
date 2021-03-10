package io.timmi.de4lroadtracker.model;

import android.hardware.Sensor;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public class AggregatedSensorValues {

    public String key;
    public final List<Float> minVals = new ArrayList<>();
    public final List<Float> maxVals = new ArrayList<>();
    public final List<Float> summedVals = new ArrayList<>();
    public final List<Integer> summedAccuracy = new ArrayList<>();
    public final List<Double> summedQuadVals = new ArrayList<>();
    public final List<Float> avgVals = new ArrayList<>();
    public final List<Float> avgAccuracy = new ArrayList<>();
    public final List<Integer> numVals = new ArrayList<>();
    @Nullable
    public Long firstTimestamp = null;
    @Nullable
    public Long firstUnixTimestampInMS = null;
    @Nullable
    public Long lastTimestamp = null;
    @Nullable
    public Long lastUnixTimestampInMS = null;
    public Sensor sensor;

    public AggregatedSensorValues(String key, Sensor sensor) {
        this.key = key;
        this.sensor = sensor;
    }
}
