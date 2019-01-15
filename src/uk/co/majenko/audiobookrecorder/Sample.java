package uk.co.majenko.audiobookrecorder;

public class Sample {
    double left;
    double right;

    public Sample(double m) {
        left = m;
        right = m;
    }

    public Sample(double l, double r) {
        left = l;
        right = r;
    }

    public double getLeft() {
        return left;
    }

    public double getRight() {
        return right;
    }

    public double getMono() {
        return (left + right) / 2.0;
    }
}
