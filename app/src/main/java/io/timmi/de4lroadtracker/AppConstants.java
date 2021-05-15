package io.timmi.de4lroadtracker;

public class AppConstants {
    final static int MQTT_BUFFER_SIZE = 10000;
    final static boolean MQTT_PERSIST_BUFFER = true;

    public final static String MQTT_PUBLISH_TOPIC_PRODUCTION = "/sensors/timmi/prod";
    public final static String MQTT_PUBLISH_TOPIC_DEBUG = "/sensors/timmi/test";
    public final static String MQTT_SUBSCRIBE_TOPIC_DEBUG = "/sensors/timmi/moo";
    public final static String MQTT_SUBSCRIBE_TOPIC_PRODUCTION = "/sensors/timmi/miau";


    public final static float TS_MINIMAL_DISTANCE_FILTER = 0.2F;
}
