package io.timmi.de4lroadtracker.helper;

import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.Nullable;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import io.timmi.de4lfilter.Filter;

public class AggregateAndFilter {

    private static final String TAG = "AggregateAndFilter";

    private static String readFileAsString(File file) {
        StringBuilder lines = new StringBuilder();
        try {
            FileInputStream fileInputStream = new FileInputStream(file);
            BufferedReader br = new BufferedReader(new InputStreamReader(fileInputStream, "ISO-8859-1"));

            String line = "";
            while ((line = br.readLine()) != null) {
                lines.append(line);
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return lines.toString();
    }

    @Nullable
    public static  String processResults(File dir,String unprocessedDir, String processedDir,  boolean removeFiles, JSONObject appInfo, JSONObject deviceInfo) {
        File sensorDataDir = new File(dir, unprocessedDir);
        File processedSensorDataDir = new File(dir, processedDir);
        if(!processedSensorDataDir.exists()) {
            processedSensorDataDir.mkdir();
        }
        if (sensorDataDir.exists()) {
            File[] files = sensorDataDir.listFiles();
            List<String> locations = new ArrayList<String>();
            List<String> sensorValues = new ArrayList<String>();

            String sensorInfos = "[]";
            File sensorInfoFile = new File(dir, "sensors_info.json");
            if(sensorInfoFile.exists()) {
                sensorInfos = readFileAsString(sensorInfoFile);
            }
            for (File file : files) {
                String filename = file.getAbsolutePath();
                File locationsFile = new File(filename.substring(0, filename.length() - 5) + "_locations.json");
                if (locationsFile.exists()) {
                    Log.i(TAG, locationsFile.getName());
                    String loc = readFileAsString(locationsFile);
                    if (loc.length() > 2) {
                        locations.add(loc);
                        String sensorValuesStringAll = readFileAsString(file);
                        sensorValues.add(sensorValuesStringAll);
                    }
                }
                if(removeFiles && file.isFile() && file.canWrite()) {
                    file.delete();
                } else {
                    file.renameTo(new File(processedSensorDataDir, file.getName()));
                }
            }

            try {
                String locationString = "[" + TextUtils.join(",", locations) + "]";
                String sensorValuesString = "[" + TextUtils.join(",", sensorValues) + "]";
                String metaDataString = "{ \"sensorsInfo\": " + sensorInfos + ", \"appInfo\": " + appInfo.toString()  + ", \"deviceInfo\": " +  deviceInfo.toString() + " }";
                return Filter.filterMany(
                        locationString,
                        sensorValuesString,
                        metaDataString,
                        "{\"speed-limit\": 1}"  // the optional 4th argument can be used to overwrite the default config
                );
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }
}
