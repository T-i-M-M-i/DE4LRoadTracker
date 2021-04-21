package io.timmi.de4lroadtracker.helper;

import android.content.Context;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class RawResourceLoader {

    static public String loadText(int resourceId, Context ctx) {
        InputStream is = ctx.getResources().openRawResource(resourceId);
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        String readLine = null;
        StringBuilder resultBuilder = new StringBuilder();

        try {
            while ((readLine = br.readLine()) != null) {
                resultBuilder.append(readLine + '\n');
            }

            is.close();
            br.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
        return resultBuilder.toString();
    }
}
