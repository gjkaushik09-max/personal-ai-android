package com.personalai;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class VoiceService extends Service {

    private static final String CHANNEL_ID = "personal_ai_voice";

    @Override
    public void onCreate() {
        super.onCreate();

        NotificationChannel channel =
                new NotificationChannel(
                        CHANNEL_ID,
                        "Personal AI Voice",
                        NotificationManager.IMPORTANCE_LOW
                );

        NotificationManager manager =
                getSystemService(NotificationManager.class);

        manager.createNotificationChannel(channel);

        Notification notification =
                new Notification.Builder(this, CHANNEL_ID)
                        .setContentTitle("Personal AI")
                        .setContentText("Voice assistant is active 🎙️")
                        .setSmallIcon(android.R.drawable.ic_btn_speak_now)
                        .build();

        startForeground(1, notification);
    }

    @Override
    public int onStartCommand(
            Intent intent,
            int flags,
            int startId
    ) {
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
