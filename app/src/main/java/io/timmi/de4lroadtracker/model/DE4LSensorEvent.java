package io.timmi.de4lroadtracker.model;

import android.hardware.Sensor;
import android.hardware.SensorEvent;

public class DE4LSensorEvent {
    public int accuracy;
    public Sensor sensor;
    public long timestamp;
    public final float[] values;

    public DE4LSensorEvent(int valueSize) {
        values = new float[valueSize];
    }
    public DE4LSensorEvent(SensorEvent ev) {
        accuracy = ev.accuracy;
        sensor = ev.sensor;
        timestamp = ev.timestamp;
        values = ev.values;
    }


}
