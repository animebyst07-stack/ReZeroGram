/*
 * ReZeroGram — debug log buffer for auth diagnostics.
 * Captures logs regardless of BuildVars.LOGS_ENABLED.
 */
package com.ZeroGram.ReZeroGram;

import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.Date;
import java.util.Locale;

public class AyuDebugLogBuffer {

    private static final int MAX_LINES = 300;
    private static final ArrayDeque<String> buffer = new ArrayDeque<>();
    private static final SimpleDateFormat fmt = new SimpleDateFormat("HH:mm:ss.SSS", Locale.US);

    public static synchronized void log(String level, String message) {
        try {
            String line = fmt.format(new Date()) + " " + level + " " + (message != null ? message : "null");
            buffer.addLast(line);
            if (buffer.size() > MAX_LINES) {
                buffer.pollFirst();
            }
        } catch (Throwable ignored) {}
    }

    public static synchronized String getLogs() {
        if (buffer.isEmpty()) return "(нет логов)";
        StringBuilder sb = new StringBuilder();
        for (String line : buffer) {
            sb.append(line).append('\n');
        }
        return sb.toString();
    }

    public static synchronized int getCount() {
        return buffer.size();
    }

    public static synchronized void clear() {
        buffer.clear();
    }
}
