package io.timmi.de4lroadtracker.helper;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import io.timmi.de4lroadtracker.R;
import io.timmi.de4lroadtracker.SensorRecorder;

public class Exporter {

    public static void shareGPX(File gpxFile, Context context, String providerAuthority) {

        Uri contentUri = FileProvider.getUriForFile(context, providerAuthority, gpxFile);
        if (contentUri == null) return;
        Intent shareIntent = new Intent(Intent.ACTION_VIEW);
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        shareIntent.setType("application/gpx+xml");
        shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
        context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.open_gpx_with)));

    }

    public static File locationFilesToGPX(File dir, File outDir) {
        File sensorDataDir = new File(dir, SensorRecorder.PROCESSED_SENSOR_DATA_DIR);

        Pattern pattern = Pattern.compile(".*locations.json$");
        List<String> locations = new ArrayList<String>();
        if (sensorDataDir.exists()) {
            File[] files = sensorDataDir.listFiles();
            for (File file : files) {
                if (pattern.matcher(file.getName()).matches() && file.exists()) {
                    String loc = AggregateAndFilter.readFileAsString(file);
                    if (loc.length() > 2) {
                        locations.add(loc);
                    }
                }
            }
        }
        try {
            String locationString = "[" + TextUtils.join(",", locations) + "]";
            String gpxData = io.timmi.de4lfilter.Exporter.exportToGpxMany(locationString);
            if (gpxData != null) {
                File file = File.createTempFile("gpx", ".gpx", outDir);
                FileOutputStream fOut = new FileOutputStream(file, true);
                fOut.write(gpxData.getBytes());
                fOut.close();
                return file;
            }
        } catch (Exception ignored) {
        }
        return null;
    }
}
