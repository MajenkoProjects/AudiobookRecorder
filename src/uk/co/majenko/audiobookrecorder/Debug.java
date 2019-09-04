package uk.co.majenko.audiobookrecorder;

public class Debug {
    static long timestamp;

    static void debug(String msg) {
        long now = System.currentTimeMillis();
        long diff = now - timestamp;
        timestamp = now;
        System.err.println(String.format("%8d - %s", diff, msg));
    }
}
