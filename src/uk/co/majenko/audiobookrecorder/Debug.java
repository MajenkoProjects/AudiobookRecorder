package uk.co.majenko.audiobookrecorder;

import java.util.Date;
import java.text.SimpleDateFormat;

public class Debug {
    static long timestamp;
    static public boolean debugEnabled = false;
    static public boolean traceEnabled = false;

    static void debug(String msg) {
        if (!debugEnabled) return;
        long now = System.currentTimeMillis();
        long diff = now - timestamp;
        timestamp = now;
        System.err.println(String.format("%8d - %s", diff, msg));
    }

    static void d(Object... args) {
        if (!debugEnabled) return;

        Thread t = Thread.currentThread();
        StackTraceElement[] st = t.getStackTrace();
        StackTraceElement caller = st[2];

        String tag = "[" + getCurrentLocalDateTimeStamp() + "] " + caller.getFileName() + " " + caller.getLineNumber() + " (" + caller.getMethodName() + "):";

        System.err.print(tag);

        for (Object o : args) {
            System.err.print(" ");
            System.err.print(o);
        }
        System.err.println();
    }

    static void trace() {
        if (!traceEnabled) return;
        Thread t = Thread.currentThread();
        StackTraceElement[] st = t.getStackTrace();
        StackTraceElement caller = st[3];
        StackTraceElement callee = st[2];

        String tag = "[" + getCurrentLocalDateTimeStamp() + "] " + t.getName() + " - " + caller.getFileName() + ":" + caller.getLineNumber() + " " + caller.getMethodName() + "(...) -> " + callee.getFileName() + ":" + callee.getLineNumber() + " " + callee.getMethodName() + "(...)";

        System.err.println(tag);
    }

    public static String getCurrentLocalDateTimeStamp() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date());
    }

}
