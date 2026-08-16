package com.personalai.command;

import java.util.Locale;

public class CommandEngine {

    public enum Type {
        OPEN_APP,
        CALCULATE,
        WEB_SEARCH,
        LEARN,
        STOP,
        UNKNOWN
    }

    public static class Command {
        public final Type type;
        public final String value;

        public Command(Type type, String value) {
            this.type = type;
            this.value = value;
        }
    }

    public Command parse(String input) {

        if (input == null || input.trim().isEmpty()) {
            return new Command(Type.UNKNOWN, "");
        }

        String command =
                input.trim().toLowerCase(Locale.ROOT);

        // STOP
        if (command.equals("stop") ||
                command.equals("બંધ") ||
                command.equals("બંધ કરો") ||
                command.equals("stop listening")) {

            return new Command(
                    Type.STOP,
                    ""
            );
        }

        // LEARN
        if (command.startsWith("learn ")) {

            return new Command(
                    Type.LEARN,
                    input.substring(6).trim()
            );
        }

        // OPEN APP
        String[] openPrefixes = {
                "open ",
                "launch ",
                "start ",
                "ખોલ ",
                "ખોલો ",
                "ચાલુ કરો "
        };

        for (String prefix : openPrefixes) {

            if (command.startsWith(prefix)) {

                String app =
                        input.substring(prefix.length())
                                .trim();

                return new Command(
                        Type.OPEN_APP,
                        app
                );
            }
        }

        // OPEN APP - suffix
        String[] openSuffixes = {
                " open",
                " ખોલ",
                " ખોલો",
                " ચાલુ કરો"
        };

        for (String suffix : openSuffixes) {

            if (command.endsWith(suffix)) {

                String app =
                        input.substring(
                                0,
                                input.length()
                                        - suffix.length()
                        ).trim();

                return new Command(
                        Type.OPEN_APP,
                        app
                );
            }
        }

        // CALCULATOR
        if (containsMath(command) ||
                command.contains("calculate") ||
                command.contains("calculator") ||
                command.contains("કેટલા") ||
                command.contains("ગણતરી")) {

            return new Command(
                    Type.CALCULATE,
                    input
            );
        }

        // WEB SEARCH
        if (command.startsWith("search ") ||
                command.startsWith("google ") ||
                command.startsWith("find ") ||
                command.contains("internet પર") ||
                command.contains("શોધ")) {

            return new Command(
                    Type.WEB_SEARCH,
                    input
            );
        }

        return new Command(
                Type.UNKNOWN,
                input
        );
    }

    private boolean containsMath(
            String command) {

        return command.matches(
                ".*[0-9].*[+\\-*/×÷=].*[0-9].*"
        );
    }
}
