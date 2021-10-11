package io.timmi.de4lroadtracker.model;

import android.hardware.Sensor;
import android.hardware.SensorEvent;

import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "SensorEvent")
public class DE4LSensorEvent {

    public String key;
    public int accuracy;
    public Sensor sensor;
    public long timestamp;
    public final float[] values;


    public DE4LSensorEvent(String key, int accuracy, Sensor sensor, long timestamp, float[] values) {
        this.key = key;
        this.accuracy = accuracy;
        this.sensor = sensor;
        this.values = new float[values.length];
        System.arraycopy(values, 0, this.values, 0, values.length);
        this.timestamp = timestamp;
    }

    public DE4LSensorEvent(String key, SensorEvent ev) {
        this.key = key;
        accuracy = ev.accuracy;
        sensor = ev.sensor;
        timestamp = ev.timestamp;
        values = new float[ev.values.length];
        System.arraycopy(ev.values, 0, values, 0, ev.values.length);
    }


}
