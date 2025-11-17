package com.spotifymod.debug;

import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.Date;
import java.util.Deque;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Simple in-memory ring buffer logger for debug GUI.
 */
public class LogBuffer {
    public enum Level { TRACE, INFO, WARN, ERROR }

    private static final int MAX_ENTRIES = 500;
    private static final LogBuffer INSTANCE = new LogBuffer();
    private final Deque<LogEntry> entries = new ArrayDeque<>();
    private final ReentrantLock lock = new ReentrantLock();
    private final SimpleDateFormat fmt = new SimpleDateFormat("HH:mm:ss.SSS");

    public static LogBuffer get() { return INSTANCE; }

    public static class LogEntry {
        public final long ts;
        public final Level level;
        public final String message;
        public LogEntry(long ts, Level level, String message) {
            this.ts = ts;
            this.level = level;
            this.message = message;
        }
        public String formatted(SimpleDateFormat fmt) {
            return fmt.format(new Date(ts)) + " [" + level + "] " + message;
        }
    }

    private void add(Level level, String msg) {
        lock.lock();
        try {
            if (entries.size() >= MAX_ENTRIES) {
                entries.removeFirst();
            }
            entries.addLast(new LogEntry(System.currentTimeMillis(), level, msg));
        } finally {
            lock.unlock();
        }
    }

    public void trace(String msg) { add(Level.TRACE, msg); }
    public void info(String msg) { add(Level.INFO, msg); }
    public void warn(String msg) { add(Level.WARN, msg); }
    public void error(String msg) { add(Level.ERROR, msg); }

    public LogEntry[] snapshot() {
        lock.lock();
        try {
            return entries.toArray(new LogEntry[0]);
        } finally {
            lock.unlock();
        }
    }

    public void clear() {
        lock.lock();
        try { entries.clear(); } finally { lock.unlock(); }
    }
}
