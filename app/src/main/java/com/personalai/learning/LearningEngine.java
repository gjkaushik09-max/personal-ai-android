package com.personalai.learning;

import android.content.Context;
import android.content.SharedPreferences;

public class LearningEngine {

    private final SharedPreferences memory;

    public LearningEngine(Context context) {
        memory = context.getSharedPreferences(
                "personal_ai_memory",
                Context.MODE_PRIVATE
        );
    }

    public void teach(String command, String action) {
        if (command == null || action == null) return;

        command = command.trim().toLowerCase();
        action = action.trim();

        if (command.isEmpty() || action.isEmpty()) return;

        memory.edit()
                .putString("command_" + command, action)
                .apply();
    }

    public String learn(String command) {
        if (command == null) return null;

        return memory.getString(
                "command_" + command.trim().toLowerCase(),
                null
        );
    }

    public boolean hasLearned(String command) {
        return learn(command) != null;
    }
}
