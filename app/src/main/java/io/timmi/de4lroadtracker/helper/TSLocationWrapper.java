package io.timmi.de4lroadtracker.helper;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import androidx.annotation.Nullable;

import com.google.android.gms.location.LocationRequest;
import com.transistorsoft.locationmanager.adapter.BackgroundGeolocation;
import com.transistorsoft.locationmanager.adapter.TSConfig;
import com.transistorsoft.locationmanager.adapter.callback.TSCallback;
import com.transistorsoft.locationmanager.adapter.callback.TSLocationCallback;
import com.transistorsoft.locationmanager.location.TSLocation;


import io.timmi.de4lroadtracker.AppConstants;
import io.timmi.de4lroadtracker.BuildConfig;

import static io.timmi.de4lroadtracker.AppConstants.*;

public class TSLocationWrapper implements SharedPreferences.OnSharedPreferenceChangeListener {
    private final String TAG = "TSLocationWrapper";
    private SharedPreferences settings;

    @Nullable
    public BackgroundGeolocation bgGeo;
    @Nullable
    private TSConfig config;

    public TSLocationWrapper(Context ctx) {
        initializeBGLocation(ctx);
    }

    public void startBG() {
        if (bgGeo != null) {
            bgGeo.start();
        }
    }
    public void stopBG() {
        if (bgGeo != null) {
            bgGeo.stop();
        }
    }

    private void initializeBGLocation(Context ctx) {
        settings = PreferenceManager.getDefaultSharedPreferences(ctx);
        settings.registerOnSharedPreferenceChangeListener(this);
        bgGeo = BackgroundGeolocation.getInstance(ctx);
        config = TSConfig.getInstance(ctx);

        // Configure the SDK
        TSConfig.Builder builder = config.updateWithBuilder()
                .setDebug(settings.getBoolean("sound", false)) // Sound Fx / notifications during development
                .setLogLevel(BuildConfig.DEBUG ? 5 : 1) // Verbose logging during development
                .setDesiredAccuracy(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setDistanceFilter(AppConstants.TS_MINIMAL_DISTANCE_FILTER)
                .setStopTimeout(1L)
                .setHeartbeatInterval(60)
                .setStopOnTerminate(true)
                .setForegroundService(true)
                .setStartOnBoot(false);

        String debugUrl = settings.getString("locationServiceUrl", "");

        if (!debugUrl.isEmpty()) {
            builder.setUrl(debugUrl);
        }
        builder.commit();

        bgGeo.ready(new TSCallback() {
            @Override
            public void onSuccess() {
                Log.i(TAG, "[ready] success");
                if (config.getEnabled()) {
                    Log.d(TAG, "[ready] location services config tells it's already enabled");
                }
                //try {
                //    bgGeo.start();
                //} catch (Exception e) {
                //    Log.e(TAG, "cannot start background location tracker" + e.getMessage(), e);
                //}
            }

            @Override
            public void onFailure(String error) {
                Log.i(TAG, "[ready] FAILURE: " + error);
            }
        });

    }

    private void updateTSDebugUrl(String debugUrl) {

        if (config == null) {
            return;
        }
        TSConfig.Builder builder = config.updateWithBuilder();
        builder.setUrl(debugUrl);
        builder.commit();
    }
    private void updateTSSound(boolean withSound) {
        if (config == null) {
            return;
        }
        TSConfig.Builder builder = config.updateWithBuilder();
        builder.setDebug(withSound); // Sound Fx / notifications during development
        builder.commit();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
        switch (s) {
            case "sound":
                updateTSSound(settings.getBoolean("sound", false));
                break;
            case "locationServiceUrl":
                updateTSDebugUrl(settings.getString("locationServiceUrl", ""));
                break;
            default:
        }
    }
}
