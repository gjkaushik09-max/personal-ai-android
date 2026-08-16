package com.personalai;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;

import java.util.ArrayList;
import java.util.Locale;

public class VoiceService extends Service {

    private static final String CHANNEL_ID = "personal_ai_voice";

    private SpeechRecognizer speechRecognizer;
    private Intent speechIntent;

    @Override
    public void onCreate() {
        super.onCreate();

        createNotification();

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);

        speechRecognizer.setRecognitionListener(new RecognitionListener() {

            @Override
            public void onResults(Bundle results) {
                ArrayList<String> matches =
                        results.getStringArrayList(
                                SpeechRecognizer.RESULTS_RECOGNITION
                        );

                if (matches != null && !matches.isEmpty()) {
                    String command = matches.get(0);
                    handleCommand(command);
                }

                listenAgain();
            }

            @Override
            public void onError(int error) {
                listenAgain();
            }

            @Override public void onReadyForSpeech(Bundle params) {}
            @Override public void onBeginningOfSpeech() {}
            @Override public void onRmsChanged(float rmsdB) {}
            @Override public void onBufferReceived(byte[] buffer) {}
            @Override public void onEndOfSpeech() {}
            @Override public void onPartialResults(Bundle partialResults) {}
            @Override public void onEvent(int eventType, Bundle params) {}
        });

        speechIntent = new Intent(
                RecognizerIntent.ACTION_RECOGNIZE_SPEECH
        );

        speechIntent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        );

        speechIntent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE,
                Locale.getDefault()
        );

        speechIntent.putExtra(
                RecognizerIntent.EXTRA_PARTIAL_RESULTS,
                true
        );

        listenAgain();
    }

    private void listenAgain() {
        if (speechRecognizer != null) {
            speechRecognizer.startListening(speechIntent);
        }
    }

    private void handleCommand(String command) {

        String lower = command.toLowerCase(Locale.ROOT);

        if (lower.contains("youtube") ||
                command.contains("યૂટ્યુબ")) {

            Intent intent = new Intent(
                    Intent.ACTION_VIEW,
                    android.net.Uri.parse("https://www.youtube.com")
            );

            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }

        else if (lower.contains("chrome")) {

            Intent intent = getPackageManager()
                    .getLaunchIntentForPackage(
                            "com.android.chrome"
                    );

            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }
        }

        else if (lower.contains("google")) {

            Intent intent = new Intent(
                    Intent.ACTION_VIEW,
                    android.net.Uri.parse("https://www.google.com")
            );

            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }
    }

    private void createNotification() {

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
                        .setContentText(
                                "Listening for voice commands 🎙️"
                        )
                        .setSmallIcon(
                                android.R.drawable.ic_btn_speak_now
                        )
                        .setOngoing(true)
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
    public void onDestroy() {

        if (speechRecognizer != null) {
            speechRecognizer.destroy();
            speechRecognizer = null;
        }

        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
