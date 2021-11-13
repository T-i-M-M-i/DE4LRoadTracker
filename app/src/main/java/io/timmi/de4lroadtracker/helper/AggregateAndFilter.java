package io.timmi.de4lroadtracker.helper;

import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.Nullable;

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
    public static  String processResults(File dir, Boolean removeFiles) {
        if (dir.exists()) {
            File[] files = dir.listFiles();
            List<String> locations = new ArrayList<String>();
            List<String> sensorValues = new ArrayList<String>();
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
                }
            }

            try {
                return Filter.filterMany(
                        "[" + TextUtils.join(",", locations) + "]",
                        "[" + TextUtils.join(",", sensorValues) + "]",
                        "{}",
                        "{\"speed-limit\": 1}"  // the optional 4th argument can be used to overwrite the default config
                );
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }
}
