package io.timmi.de4lroadtracker.helper;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.Settings;

import org.json.JSONException;
import org.json.JSONObject;

public class JsonInfoBuilder {

    public static JSONObject getAppInfo(Context context) {
        JSONObject res = new JSONObject();
        try {
            PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            res.put("version", pInfo.versionName);
            res.put("versionCode", pInfo.versionCode);
            res.put("packageName", pInfo.packageName);
        } catch (PackageManager.NameNotFoundException | JSONException e) {
            e.printStackTrace();
        }
        return res;
    }

    @SuppressLint("HardwareIds")
    public static JSONObject getDeviceInfo(Context appContext) {
        JSONObject deviceInfo = new JSONObject();
        try {
            deviceInfo.put("android_id",
                    appContext == null
                            ? "not yet available"
                            : Settings.Secure.getString(appContext.getContentResolver(), Settings.Secure.ANDROID_ID));
            deviceInfo.put("manufacturer", Build.MANUFACTURER);
            deviceInfo.put("model", Build.MODEL);
            deviceInfo.put("device", Build.DEVICE);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return deviceInfo;

    }
}
