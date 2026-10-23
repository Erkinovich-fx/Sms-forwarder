package com.yourname.smsforwarder;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

/**
 * Doimiy fon jarayoni. O'chmaydigan (doimiy) notification ko'rsatadi,
 * shu orqali Android tizimiga "bu ilova faol ishlamoqda, uni to'xtatma"
 * degan signal beriladi - xuddi musiqa pleyerlari kabi.
 */
public class KeepAliveService extends Service {

    private static final String CHANNEL_ID = "sms_forwarder_channel";
    private static final int NOTIFICATION_ID = 1;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        startForeground(NOTIFICATION_ID, buildNotification());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // START_STICKY - tizim xotira yetishmasligi sababli xizmatni
        // to'xtatsa ham, imkon qadar tezroq qayta ishga tushiradi
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "SMS Forwarder",
                NotificationManager.IMPORTANCE_MIN // iloji boricha kam e'tiborni tortadigan
            );
            channel.setDescription("SMS forwarder fon jarayoni faol");
            channel.setShowBadge(false);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    private Notification buildNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("SMS Forwarder faol")
            .setContentText("Kelgan SMS'lar avtomatik yuborilmoqda")
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .build();
    }
}
