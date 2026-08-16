package com.personalai;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;

import com.personalai.learning.LearningEngine;

import java.util.ArrayList;
import java.util.Locale;

public class VoiceService extends Service {

    private static final String CHANNEL_ID = "personal_ai_voice";

    private SpeechRecognizer speechRecognizer;
    private Intent speechIntent;
    private LearningEngine learningEngine;
    private Handler handler;

    @Override
    public void onCreate() {
        super.onCreate();

        handler = new Handler();
        learningEngine = new LearningEngine(this);

        createNotification();

        speechRecognizer =
                SpeechRecognizer.createSpeechRecognizer(this);

        speechIntent =
                new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);

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

        speechRecognizer.setRecognitionListener(
                new RecognitionListener() {

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

                restartListening();
            }

            @Override
            public void onError(int error) {
                restartListening();
            }

            @Override
            public void onEndOfSpeech() {
                // Wait for results.
            }

            @Override public void onReadyForSpeech(Bundle params) {}
            @Override public void onBeginningOfSpeech() {}
            @Override public void onRmsChanged(float rmsdB) {}
            @Override public void onBufferReceived(byte[] buffer) {}
            @Override public void onPartialResults(Bundle partialResults) {}
            @Override public void onEvent(int eventType, Bundle params) {}
        });

        startListening();
    }

    private void startListening() {

        if (speechRecognizer != null) {
            speechRecognizer.startListening(speechIntent);
        }
    }

    private void restartListening() {

        handler.postDelayed(() -> {

            if (speechRecognizer != null) {
                startListening();
            }

        }, 3000);
    }

    private void handleCommand(String command) {

        if (command == null) return;

        String lower =
                command.toLowerCase(Locale.ROOT).trim();

        // =========================
        // LEARNING COMMAND
        // =========================

        if (lower.startsWith("learn ")) {

            String data = command.substring(6);

            String[] parts = data.split("=", 2);

            if (parts.length == 2) {

                String learnedCommand =
                        parts[0].trim();

                String action =
                        parts[1].trim();

                learningEngine.teach(
                        learnedCommand,
                        action
                );
            }

            return;
        }

        // =========================
        // LEARNED COMMAND
        // =========================

        String learnedAction =
                learningEngine.learn(command);

        if (learnedAction != null) {

            executeAction(learnedAction);

            return;
        }

        // =========================
        // BUILT-IN COMMANDS
        // =========================

        if (lower.contains("youtube") ||
                lower.contains("યૂટ્યુબ")) {

            openUrl(
                    "https://www.youtube.com"
            );

        } else if (lower.contains("google")) {

            openUrl(
                    "https://www.google.com"
            );

        } else if (lower.contains("chrome")) {

            Intent intent =
                    getPackageManager()
                            .getLaunchIntentForPackage(
                                    "com.android.chrome"
                            );

            if (intent != null) {

                intent.addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK
                );

                startActivity(intent);
            }
        }
    }

    private void executeAction(String action) {

        if (action == null) return;

        if (action.equalsIgnoreCase(
                "OPEN_YOUTUBE")) {

            openUrl(
                    "https://www.youtube.com"
            );

        } else if (action.equalsIgnoreCase(
                "OPEN_GOOGLE")) {

            openUrl(
                    "https://www.google.com"
            );
        }
    }

    private void openUrl(String url) {

        Intent intent =
                new Intent(
                        Intent.ACTION_VIEW,
                        android.net.Uri.parse(url)
                );

        intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
        );

        startActivity(intent);
    }

    private void createNotification() {

        NotificationChannel channel =
                new NotificationChannel(
                        CHANNEL_ID,
                        "Personal AI Voice",
                        NotificationManager
                                .IMPORTANCE_LOW
                );

        NotificationManager manager =
                getSystemService(
                        NotificationManager.class
                );

        manager.createNotificationChannel(channel);

        Notification notification =
                new Notification.Builder(
                        this,
                        CHANNEL_ID
                )
                .setContentTitle(
                        "Personal AI"
                )
                .setContentText(
                        "Listening for commands 🎙️"
                )
                .setSmallIcon(
                        android.R.drawable
                                .ic_btn_speak_now
                )
                .setOngoing(true)
                .build();

        startForeground(
                1,
                notification
        );
    }

    @Override
    public void onDestroy() {

        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }

        if (speechRecognizer != null) {
            speechRecognizer.destroy();
            speechRecognizer = null;
        }

        super.onDestroy();
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
