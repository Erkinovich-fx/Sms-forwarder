package com.yourname.smsforwarder;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.getcapacitor.annotation.Permission;
import com.getcapacitor.annotation.PermissionCallback;

@CapacitorPlugin(
    name = "SmsForwarder",
    permissions = {
        @Permission(strings = { Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS }, alias = "sms")
    }
)
public class SmsForwarderPlugin extends Plugin {

    public static final String PREFS = "sms_forwarder_prefs";

    @PluginMethod
    public void setConfig(PluginCall call) {
        String token = call.getString("token");
        String chatId = call.getString("chatId");

        SharedPreferences prefs = getContext().getSharedPreferences(PREFS, 0);
        prefs.edit()
            .putString("bot_token", token)
            .putString("chat_id", chatId)
            .apply();

        if (getPermissionState("sms") != com.getcapacitor.PermissionState.GRANTED) {
            requestPermissionForAlias("sms", call, "smsPermsCallback");
        } else {
            JSObject ret = new JSObject();
            ret.put("granted", true);
            call.resolve(ret);
        }
    }

    @PluginMethod
    public void getStatus(PluginCall call) {
        SharedPreferences prefs = getContext().getSharedPreferences(PREFS, 0);
        JSObject ret = new JSObject();
        ret.put("chatId", prefs.getString("chat_id", MainActivity.DEFAULT_CHAT_ID));
        ret.put("granted", getPermissionState("sms") == com.getcapacitor.PermissionState.GRANTED);
        ret.put("batteryOptimized", isBatteryOptimized());
        call.resolve(ret);
    }

    @PluginMethod
    public void requestIgnoreBatteryOptimization(PluginCall call) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            String packageName = getContext().getPackageName();
            Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            intent.setData(Uri.parse("package:" + packageName));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            getContext().startActivity(intent);
        }
        call.resolve();
    }

    private boolean isBatteryOptimized() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager pm = (PowerManager) getContext().getSystemService(Context.POWER_SERVICE);
            if (pm != null) {
                return !pm.isIgnoringBatteryOptimizations(getContext().getPackageName());
            }
        }
        return false;
    }

    @PermissionCallback
    private void smsPermsCallback(PluginCall call) {
        JSObject ret = new JSObject();
        ret.put("granted", getPermissionState("sms") == com.getcapacitor.PermissionState.GRANTED);
        call.resolve(ret);
    }
}
