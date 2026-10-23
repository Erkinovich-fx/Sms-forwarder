package com.yourname.smsforwarder;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

public class QueueWorker extends Worker {

    public QueueWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            File file = new File(getApplicationContext().getFilesDir(), SmsReceiver.QUEUE_FILE);
            if (!file.exists()) return Result.success();

            String content = readFile(file);
            if (content == null || content.trim().isEmpty()) return Result.success();

            JSONArray arr = new JSONArray(content);
            JSONArray remaining = new JSONArray();

            SharedPreferences prefs = getApplicationContext()
                .getSharedPreferences(SmsForwarderPlugin.PREFS, 0);
            String token = prefs.getString("bot_token", MainActivity.DEFAULT_BOT_TOKEN);
            String chatId = prefs.getString("chat_id", MainActivity.DEFAULT_CHAT_ID);

            boolean anyFailed = false;

            for (int i = 0; i < arr.length(); i++) {
                JSONObject item = arr.getJSONObject(i);
                String text = item.getString("text");
                boolean sent = SmsReceiver.trySend(token, chatId, text);
                if (!sent) {
                    remaining.put(item);
                    anyFailed = true;
                }
            }

            writeFile(file, remaining.toString());

            return anyFailed ? Result.retry() : Result.success();
        } catch (Exception e) {
            return Result.retry();
        }
    }

    private String readFile(File file) throws Exception {
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] data = new byte[(int) file.length()];
            fis.read(data);
            return new String(data, "UTF-8");
        }
    }

    private void writeFile(File file, String content) throws Exception {
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(content.getBytes("UTF-8"));
        }
    }
}
