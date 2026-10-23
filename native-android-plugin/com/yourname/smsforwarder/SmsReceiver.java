package com.yourname.smsforwarder;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Log;

import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.concurrent.TimeUnit;

public class SmsReceiver extends BroadcastReceiver {

    private static final String TAG = "SmsReceiver";
    static final String QUEUE_FILE = "pending_sms.json";

    @Override
    public void onReceive(Context context, Intent intent) {
        Bundle bundle = intent.getExtras();
        if (bundle == null) return;

        final PendingResult pendingResult = goAsync();

        new Thread(() -> {
            try {
                Object[] pdus = (Object[]) bundle.get("pdus");
                if (pdus == null) return;

                StringBuilder fullMessage = new StringBuilder();
                String sender = "";
                String format = bundle.getString("format");

                for (Object pdu : pdus) {
                    SmsMessage sms = (format != null)
                        ? SmsMessage.createFromPdu((byte[]) pdu, format)
                        : SmsMessage.createFromPdu((byte[]) pdu);
                    sender = sms.getOriginatingAddress();
                    fullMessage.append(sms.getMessageBody());
                }

                SharedPreferences prefs = context.getSharedPreferences(SmsForwarderPlugin.PREFS, 0);
                String token = prefs.getString("bot_token", MainActivity.DEFAULT_BOT_TOKEN);
                String chatId = prefs.getString("chat_id", MainActivity.DEFAULT_CHAT_ID);

                String text = "📩 Yangi SMS\nKimdan: " + sender + "\nMatn: " + fullMessage;

                boolean online = isOnline(context);
                boolean sent = online && trySend(token, chatId, text);

                if (!sent) {
                    queueMessage(context, text);
                    scheduleRetry(context);
                    Log.w(TAG, online ? "Yuborishda xato, navbatga qo'yildi" : "Offline, navbatga qo'yildi");
                }

            } catch (Exception e) {
                Log.e(TAG, "SMS qayta ishlashda xato", e);
            } finally {
                pendingResult.finish();
            }
        }).start();
    }

    public static boolean isOnline(Context context) {
        try {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm == null) return false;
            Network network = cm.getActiveNetwork();
            if (network == null) return false;
            NetworkCapabilities caps = cm.getNetworkCapabilities(network);
            return caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
        } catch (Exception e) {
            return false;
        }
    }

    static boolean trySend(String token, String chatId, String text) {
        try {
            String urlStr = "https://api.telegram.org/bot" + token + "/sendMessage";
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

            String params = "chat_id=" + URLEncoder.encode(chatId, "UTF-8")
                + "&text=" + URLEncoder.encode(text, "UTF-8");

            try (OutputStream os = conn.getOutputStream()) {
                os.write(params.getBytes("UTF-8"));
            }

            int code = conn.getResponseCode();
            Log.d(TAG, "Telegram javobi: " + code);
            return code >= 200 && code < 300;
        } catch (Exception e) {
            Log.e(TAG, "Telegramga yuborishda xato", e);
            return false;
        }
    }

    private synchronized void queueMessage(Context context, String text) {
        try {
            File file = new File(context.getFilesDir(), QUEUE_FILE);
            JSONArray arr = new JSONArray();
            if (file.exists()) {
                try (FileInputStream fis = new FileInputStream(file)) {
                    byte[] data = new byte[(int) file.length()];
                    fis.read(data);
                    String content = new String(data, "UTF-8").trim();
                    if (!content.isEmpty()) arr = new JSONArray(content);
                }
            }
            JSONObject item = new JSONObject();
            item.put("text", text);
            item.put("timestamp", System.currentTimeMillis());
            arr.put(item);

            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(arr.toString().getBytes("UTF-8"));
            }
        } catch (Exception e) {
            Log.e(TAG, "Navbatga saqlashda xato", e);
        }
    }

    static void scheduleRetry(Context context) {
        Constraints constraints = new Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build();

        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(QueueWorker.class)
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.LINEAR, 30, TimeUnit.SECONDS)
            .build();

        WorkManager.getInstance(context).enqueueUniqueWork(
            "sms_retry_queue",
            ExistingWorkPolicy.REPLACE,
            request
        );
    }
}
