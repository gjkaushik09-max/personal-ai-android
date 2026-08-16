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
import android.speech.tts.TextToSpeech;

import com.personalai.learning.LearningEngine;

import java.util.ArrayList;
import java.util.Locale;

public class VoiceService extends Service {

    private static final String CHANNEL_ID = "personal_ai_voice";

    private SpeechRecognizer speechRecognizer;
    private Intent speechIntent;
    private LearningEngine learningEngine;
    private Handler handler;
    private TextToSpeech textToSpeech;

    private boolean listening = false;
    private boolean shuttingDown = false;

    private final Runnable autoStop = () -> {
        if (listening && !shuttingDown) {
            stopListening();
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();

        handler = new Handler();
        learningEngine = new LearningEngine(this);

        createNotification();

        textToSpeech = new TextToSpeech(
                this,
                status -> {
                    if (status == TextToSpeech.SUCCESS) {
                        textToSpeech.setLanguage(Locale.getDefault());
                    }
                }
        );

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
                false
        );

        speechRecognizer.setRecognitionListener(
                new RecognitionListener() {

                    @Override
                    public void onResults(Bundle results) {

                        ArrayList<String> matches =
                                results.getStringArrayList(
                                        SpeechRecognizer.RESULTS_RECOGNITION
                                );

                        if (matches != null &&
                                !matches.isEmpty()) {

                            handleCommand(matches.get(0));
                        }

                        scheduleAutoStop();
                    }

                    @Override
                    public void onError(int error) {
                        scheduleAutoStop();
                    }

                    @Override
                    public void onEndOfSpeech() {
                        scheduleAutoStop();
                    }

                    @Override
                    public void onReadyForSpeech(Bundle params) {
                        listening = true;
                    }

                    @Override
                    public void onBeginningOfSpeech() {
                        handler.removeCallbacks(autoStop);
                    }

                    @Override
                    public void onRmsChanged(float rmsdB) {}

                    @Override
                    public void onBufferReceived(byte[] buffer) {}

                    @Override
                    public void onPartialResults(Bundle partialResults) {}

                    @Override
                    public void onEvent(
                            int eventType,
                            Bundle params) {}
                }
        );

        startListening();
    }

    private void startListening() {

        if (shuttingDown ||
                speechRecognizer == null) {
            return;
        }

        try {
            handler.removeCallbacks(autoStop);

            speechRecognizer.startListening(
                    speechIntent
            );

            listening = true;

        } catch (Exception ignored) {
        }
    }

    private void scheduleAutoStop() {

        handler.removeCallbacks(autoStop);

        handler.postDelayed(
                autoStop,
                3000
        );
    }

    private void stopListening() {

        listening = false;

        handler.removeCallbacks(autoStop);

        if (speechRecognizer != null) {
            try {
                speechRecognizer.stopListening();
            } catch (Exception ignored) {
            }
        }

        stopSelf();
    }

    private void handleCommand(String command) {

        if (command == null) {
            return;
        }

        String lower =
                command.toLowerCase(Locale.ROOT).trim();

        if (lower.equals("બંધ") ||
                lower.equals("stop") ||
                lower.equals("stop listening")) {

            speak("Okay, stopping.");

            handler.postDelayed(
                    this::stopListening,
                    1200
            );

            return;
        }

        if (lower.startsWith("learn ")) {

            String data = command.substring(6);

            String[] parts =
                    data.split("=", 2);

            if (parts.length == 2) {

                learningEngine.teach(
                        parts[0].trim(),
                        parts[1].trim()
                );

                speak("I learned that.");
            }

            return;
        }

        String learnedAction =
                learningEngine.learn(command);

        if (learnedAction != null) {

            executeAction(learnedAction);

            return;
        }

        if (lower.contains("youtube") ||
                lower.contains("યૂટ્યુબ")) {

            speak("Opening YouTube.");
            openUrl(
                    "https://www.youtube.com"
            );

        } else if (lower.contains("google")) {

            speak("Opening Google.");
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

                speak("Opening Chrome.");

                intent.addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK
                );

                startActivity(intent);

            } else {
                speak("Chrome is not installed.");
            }

        } else {

            speak(
                    "I heard you, but I don't know that command yet."
            );
        }
    }

    private void executeAction(String action) {

        if (action == null) {
            return;
        }

        if (action.equalsIgnoreCase(
                "OPEN_YOUTUBE")) {

            speak("Opening YouTube.");

            openUrl(
                    "https://www.youtube.com"
            );

        } else if (action.equalsIgnoreCase(
                "OPEN_GOOGLE")) {

            speak("Opening Google.");

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

    private void speak(String text) {

        if (textToSpeech == null) {
            return;
        }

        textToSpeech.speak(
                text,
                TextToSpeech.QUEUE_FLUSH,
                null,
                "jarvis_response"
        );
    }

    private void createNotification() {

        NotificationChannel channel =
                new NotificationChannel(
                        CHANNEL_ID,
                        "Personal AI Voice",
                        NotificationManager.IMPORTANCE_LOW
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
                .setContentTitle("Personal AI")
                .setContentText(
                        "Voice assistant active 🎙️"
                )
                .setSmallIcon(
                        android.R.drawable.ic_btn_speak_now
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

        shuttingDown = true;

        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }

        if (speechRecognizer != null) {
            speechRecognizer.destroy();
            speechRecognizer = null;
        }

        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
            textToSpeech = null;
        }

        super.onDestroy();
    }

    @Override
    public int onStartCommand(
            Intent intent,
            int flags,
            int startId
    ) {
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
