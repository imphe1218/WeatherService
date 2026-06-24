package org.weatherservice.logging;

public final class LogSanitizer {

    private LogSanitizer() {

    }

    public static String value(Object value) {
        if (value == null) {
            return "null";
        }

        return value.toString()
                .replace('\r', '_')
                .replace('\n', '_');
    }
}
