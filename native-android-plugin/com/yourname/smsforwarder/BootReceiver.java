package com.yourname.smsforwarder;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java.io.File;

public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) return;

        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())
            || "android.intent.action.QUICKBOOT_POWERON".equals(intent.getAction())) {

            // Doimiy fon xizmatini qayta ishga tushirish
            Intent serviceIntent = new Intent(context, KeepAliveService.class);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent);
            } else {
                context.startService(serviceIntent);
            }

            File file = new File(context.getFilesDir(), SmsReceiver.QUEUE_FILE);
            if (file.exists() && file.length() > 0) {
                SmsReceiver.scheduleRetry(context);
            }
        }
    }
}
