package com.personalai;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {

    private static final int MIC_PERMISSION = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        TextView title = new TextView(this);
        title.setText("🤖 Personal AI\n\nBackground Voice Assistant");
        title.setTextSize(22);
        title.setPadding(40, 60, 40, 40);

        Button start = new Button(this);
        start.setText("🎙️ Start Listening");

        Button stop = new Button(this);
        stop.setText("🛑 Stop Listening");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);

        layout.addView(title);
        layout.addView(start);
        layout.addView(stop);

        setContentView(layout);

        start.setOnClickListener(v -> startAI());
        stop.setOnClickListener(v -> stopAI());
    }

    private void startAI() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    MIC_PERMISSION
            );
            return;
        }

        Intent intent = new Intent(this, VoiceService.class);
        startForegroundService(intent);
    }

    private void stopAI() {
        stopService(new Intent(this, VoiceService.class));
    }
}
