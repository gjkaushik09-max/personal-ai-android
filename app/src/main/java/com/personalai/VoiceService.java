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

import com.personalai.apps.AppLauncher;
import com.personalai.learning.LearningEngine;
import com.personalai.command.CommandEngine;

import java.util.ArrayList;
import java.util.Locale;

public class VoiceService extends Service {

    private static final String CHANNEL_ID =
            "personal_ai_voice";

    private SpeechRecognizer speechRecognizer;
    private Intent speechIntent;

    private LearningEngine learningEngine;
    private AppLauncher appLauncher;
    private CommandEngine commandEngine;

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

        learningEngine =
                new LearningEngine(this);

        appLauncher =
                new AppLauncher(this);
        commandEngine = new CommandEngine();

        createNotification();

        textToSpeech =
                new TextToSpeech(
                        this,
                        status -> {
                            if (status ==
                                    TextToSpeech.SUCCESS) {

                                textToSpeech.setLanguage(
                                        Locale.getDefault()
                                );
                            }
                        }
                );

        speechRecognizer =
                SpeechRecognizer
                        .createSpeechRecognizer(this);

        speechIntent =
                new Intent(
                        RecognizerIntent
                                .ACTION_RECOGNIZE_SPEECH
                );

        speechIntent.putExtra(
                RecognizerIntent
                        .EXTRA_LANGUAGE_MODEL,
                RecognizerIntent
                        .LANGUAGE_MODEL_FREE_FORM
        );

        speechIntent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE,
                Locale.getDefault()
        );

        speechIntent.putExtra(
                RecognizerIntent
                        .EXTRA_PARTIAL_RESULTS,
                false
        );

        speechRecognizer
                .setRecognitionListener(
                        new RecognitionListener() {

                            @Override
                            public void onResults(
                                    Bundle results) {

                                ArrayList<String> matches =
                                        results
                                                .getStringArrayList(
                                                        SpeechRecognizer
                                                                .RESULTS_RECOGNITION
                                                );

                                if (matches != null &&
                                        !matches.isEmpty()) {

                                    handleCommand(
                                            matches.get(0)
                                    );
                                }

                                scheduleAutoStop();
                            }

                            @Override
                            public void onError(
                                    int error) {

                                scheduleAutoStop();
                            }

                            @Override
                            public void onEndOfSpeech() {

                                scheduleAutoStop();
                            }

                            @Override
                            public void onReadyForSpeech(
                                    Bundle params) {

                                listening = true;
                            }

                            @Override
                            public void onBeginningOfSpeech() {

                                handler.removeCallbacks(
                                        autoStop
                                );
                            }

                            @Override
                            public void onRmsChanged(
                                    float rmsdB) {
                            }

                            @Override
                            public void onBufferReceived(
                                    byte[] buffer) {
                            }

                            @Override
                            public void onPartialResults(
                                    Bundle partialResults) {
                            }

                            @Override
                            public void onEvent(
                                    int eventType,
                                    Bundle params) {
                            }
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

            handler.removeCallbacks(
                    autoStop
            );

            speechRecognizer.startListening(
                    speechIntent
            );

            listening = true;

        } catch (Exception ignored) {
        }
    }

    private void scheduleAutoStop() {

        handler.removeCallbacks(
                autoStop
        );

        handler.postDelayed(
                autoStop,
                3000
        );
    }

    private void stopListening() {

        listening = false;

        handler.removeCallbacks(
                autoStop
        );

        if (speechRecognizer != null) {

            try {
                speechRecognizer.stopListening();
            } catch (Exception ignored) {
            }
        }

        stopSelf();
    }

    private void handleCommand(
            String command) {

        if (command == null) {
            return;
        }

        String lower =
                command
                        .toLowerCase(Locale.ROOT)
                        .trim();

        CommandEngine.Command parsed =
                commandEngine.parse(command);

        if (parsed.type == CommandEngine.Type.CALCULATE) {
            calculateExpression(command);
            return;
        }

        if (parsed.type == CommandEngine.Type.WEB_SEARCH) {
            searchWeb(command);
            return;
        }

        /*
         * =========================
         * STOP COMMAND
         * =========================
         */

        if (lower.equals("stop") ||
                lower.equals("બંધ") ||
                lower.equals("બંધ કરો") ||
                lower.equals("stop listening")) {

            speak("Okay, stopping.");

            handler.postDelayed(
                    this::stopListening,
                    1000
            );

            return;
        }

        /*
         * =========================
         * LEARN COMMAND
         * =========================
         */

        if (lower.startsWith("learn ")) {

            String data =
                    command.substring(6);

            String[] parts =
                    data.split("=", 2);

            if (parts.length == 2) {

                String learnedCommand =
                        parts[0].trim();

                String action =
                        parts[1].trim();

                learningEngine.teach(
                        learnedCommand,
                        action
                );

                speak(
                        "I learned that command."
                );
            }

            return;
        }

        /*
         * =========================
         * APP OPENING
         * =========================
         */

        String appName =
                extractAppName(lower);

        if (appName != null &&
                !appName.isEmpty()) {

            if (appLauncher.openApp(
                    appName
            )) {

                speak(
                        "Opening " + appName
                );

                return;
            }
        }

        /*
         * =========================
         * LEARNED COMMAND
         * =========================
         */

        String learnedAction =
                learningEngine.learn(
                        command
                );

        if (learnedAction != null) {

            executeAction(
                    learnedAction
            );

            return;
        }

        /*
         * =========================
         * BUILT-IN URL COMMANDS
         * =========================
         */

        if (lower.contains("youtube") ||
                lower.contains("યૂટ્યુબ")) {

            speak("Opening YouTube.");

            openUrl(
                    "https://www.youtube.com"
            );

            return;
        }

        if (lower.contains("google")) {

            speak("Opening Google.");

            openUrl(
                    "https://www.google.com"
            );

            return;
        }

        if (lower.contains("chrome")) {

            Intent intent =
                    getPackageManager()
                            .getLaunchIntentForPackage(
                                    "com.android.chrome"
                            );

            if (intent != null) {

                speak(
                        "Opening Chrome."
                );

                intent.addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK
                );

                startActivity(intent);

            } else {

                speak(
                        "Chrome is not installed."
                );
            }

            return;
        }

        /*
         * =========================
         * UNKNOWN COMMAND
         * =========================
         */

        speak(
                "I heard you, but I don't know that command yet."
        );
    }

    private void calculateExpression(String input) {
        String expression = input
                .replace("calculate", "")
                .replace("calculator", "")
                .replace("કેટલા", "")
                .replace("ગણતરી", "")
                .replace("?", "")
                .replace("×", "*")
                .replace("÷", "/")
                .trim();

        try {
            double result = evaluateSimpleExpression(expression);

            if (result == Math.floor(result)) {
                speak(String.valueOf((long) result));
            } else {
                speak(String.valueOf(result));
            }
        } catch (Exception e) {
            speak("I could not calculate that.");
        }
    }

    private double evaluateSimpleExpression(String expression) {
        String value = expression.replace(" ", "");

        int plus = value.indexOf('+');
        if (plus > 0) {
            return Double.parseDouble(value.substring(0, plus))
                    + Double.parseDouble(value.substring(plus + 1));
        }

        int multiply = value.indexOf('*');
        if (multiply > 0) {
            return Double.parseDouble(value.substring(0, multiply))
                    * Double.parseDouble(value.substring(multiply + 1));
        }

        int divide = value.indexOf('/');
        if (divide > 0) {
            double divisor =
                    Double.parseDouble(value.substring(divide + 1));

            if (divisor == 0) {
                throw new ArithmeticException("Division by zero");
            }

            return Double.parseDouble(value.substring(0, divide))
                    / divisor;
        }

        int minus = value.indexOf('-', 1);
        if (minus > 0) {
            return Double.parseDouble(value.substring(0, minus))
                    - Double.parseDouble(value.substring(minus + 1));
        }

        return Double.parseDouble(value);
    }

    private void searchWeb(String input) {
        String query = input
                .replaceFirst("(?i)^search\\s+", "")
                .replaceFirst("(?i)^google\\s+", "")
                .replaceFirst("(?i)^find\\s+", "")
                .trim();

        if (query.isEmpty()) {
            speak("What should I search?");
            return;
        }

        speak("Searching.");

        Intent intent = new Intent(
                Intent.ACTION_VIEW,
                android.net.Uri.parse(
                        "https://www.google.com/search?q="
                                + android.net.Uri.encode(query)
                )
        );

        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    private String extractAppName(
            String command) {

        String value =
                command.trim();

        String[] prefixes = {
                "open ",
                "launch ",
                "start ",
                "ખોલ ",
                "ખોલો ",
                "ચાલુ કરો "
        };

        for (String prefix : prefixes) {

            if (value.startsWith(prefix)) {

                return value
                        .substring(prefix.length())
                        .trim();
            }
        }

        String[] suffixes = {
                " open",
                " ખોલ",
                " ખોલો",
                " ચાલુ કરો"
        };

        for (String suffix : suffixes) {

            if (value.endsWith(suffix)) {

                return value
                        .substring(
                                0,
                                value.length()
                                        - suffix.length()
                        )
                        .trim();
            }
        }

        return null;
    }

    private void executeAction(
            String action) {

        if (action == null) {
            return;
        }

        if (action.equalsIgnoreCase(
                "OPEN_YOUTUBE")) {

            speak(
                    "Opening YouTube."
            );

            openUrl(
                    "https://www.youtube.com"
            );

        } else if (action.equalsIgnoreCase(
                "OPEN_GOOGLE")) {

            speak(
                    "Opening Google."
            );

            openUrl(
                    "https://www.google.com"
            );
        }
    }

    private void openUrl(
            String url) {

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

    private void speak(
            String text) {

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
                        NotificationManager
                                .IMPORTANCE_LOW
                );

        NotificationManager manager =
                getSystemService(
                        NotificationManager.class
                );

        manager.createNotificationChannel(
                channel
        );

        Notification notification =
                new Notification.Builder(
                        this,
                        CHANNEL_ID
                )
                .setContentTitle(
                        "Personal AI"
                )
                .setContentText(
                        "Voice assistant active 🎙️"
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

        shuttingDown = true;

        if (handler != null) {

            handler.removeCallbacksAndMessages(
                    null
            );
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
            int startId) {

        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(
            Intent intent) {

        return null;
    }
}
