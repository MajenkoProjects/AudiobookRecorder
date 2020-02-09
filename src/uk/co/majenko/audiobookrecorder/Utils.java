package uk.co.majenko.audiobookrecorder;

import java.text.SimpleDateFormat;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.RenderingHints;
import java.net.URI;
import java.awt.Graphics2D;
import java.awt.Desktop;
import java.util.Date;
import java.util.TimeZone;

public class Utils {
    public static Image getScaledImage(Image srcImg, int w, int h){
        BufferedImage resizedImg = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = resizedImg.createGraphics();

        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.drawImage(srcImg, 0, 0, w, h, null);
        g2.dispose();

        return resizedImg;
    }

    public static boolean s2b(String s) {
        if (s == null) return false;
        if (s.equals("true")) return true;
        if (s.equals("t")) return true;
        if (s.equals("yes")) return true;
        if (s.equals("y")) return true;
        return false;
    }

    public static int s2i(String s) {
        try {
            return Integer.parseInt(s);
        } catch (Exception e) {
        }
        return 0;
    }

    public static float s2f(String s) {
        try {
            return Float.parseFloat(s);
        } catch (Exception e) {
        }
        return 0.0f;
    }

    public static double s2d(String s) {
        try {
            return Double.parseDouble(s);
        } catch (Exception e) {
        }
        return 0.0d;
    }

    public static void browse(String url) {
        if (Desktop.isDesktopSupported()) {
            try {
                Desktop.getDesktop().browse(new URI(url));
            } catch (Exception ex) {
            }
        }
    }

    static long millis = System.currentTimeMillis();

    public static void report(String tag) {
        long t = System.currentTimeMillis();
        long d = t - millis;
        millis = t;
        Debug.d(String.format("%10d - %10s : %8d | %8d | %8d", d, tag,
            Runtime.getRuntime().totalMemory(),
            Runtime.getRuntime().maxMemory(),
            Runtime.getRuntime().freeMemory()
        ));
    }

    public static String secToTime(double sec, String fmt) {
        Date d = new Date((long)(sec * 1000d));
        SimpleDateFormat df = new SimpleDateFormat(fmt); 
        df.setTimeZone(TimeZone.getTimeZone("GMT"));
        String time = df.format(d); 
        return time;
    }

    public static double[] stereoToMono(double[][] in) {
        double[] out = new double[in[Sentence.LEFT].length];

        for (int i = 0; i < in[Sentence.LEFT].length; i++) {
            out[i] = mix(in[Sentence.LEFT][i], in[Sentence.RIGHT][i]);
        }
        return out;
    }

    public static double mix(double a, double b) {
        double out;

        if ((a < 0) && (b < 0)) {
            out = (a + b) - (a * b);
        } else if ((a > 0) && (b > 0)) {
            out = (a + b) - (a * b);
        } else {
            out = a + b;
        }

        return out;
    }

}
