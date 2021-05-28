package io.timmi.de4lroadtracker.helper;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import io.timmi.de4lroadtracker.MainActivity;

public class DebugChannel {

    public static void sendHistoryBroadcast(Context context, String tag, String message)
    {
        try
        {
            Intent broadCastIntent = new Intent();
            broadCastIntent.setAction(MainActivity.HISTORY_MESSAGE_BROADCAST);
            broadCastIntent.putExtra("historyMessage", message);
            Log.d(tag, "[sendBroadcast] " + message);
            if (context != null) {
                context.sendBroadcast(broadCastIntent);
            }

        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }
}
