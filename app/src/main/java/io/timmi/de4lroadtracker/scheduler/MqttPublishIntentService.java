package io.timmi.de4lroadtracker.scheduler;

import android.app.IntentService;
import android.content.Intent;
import android.content.Context;
import android.util.Log;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 */
public class MqttPublishIntentService extends IntentService {

    private static final String TAG = "MqttPublishIntentSrv";


    public MqttPublishIntentService() {
        super("MqttPublishIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            Log.i(TAG, "I would now send the data...");
        }
    }

}