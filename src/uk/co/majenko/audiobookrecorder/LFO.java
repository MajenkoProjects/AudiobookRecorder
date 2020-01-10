package uk.co.majenko.audiobookrecorder;

import java.util.ArrayList;

public class LFO implements Effect {
    
    double phase;
    double frequency;
    double depth;
    double sampleRate;
    double sampleStep;
    int waveform;
    double duty;
    int mode;

    public static final int SINE = 0;
    public static final int COSINE = 1;
    public static final int SQUARE = 2;
    public static final int TRIANGLE = 3;
    public static final int SAWTOOTH = 4;

    public static final int ADD = 0;
    public static final int REPLACE = 1;

    public LFO(double f, double d) {
        this(f, d, 0, SINE, Math.PI, REPLACE);
    }

    public LFO(double f, double d, double p) {
        this(f, d, p, SINE, Math.PI, REPLACE);
    }

    public LFO(double f, double d, double p, int w) {
        this(f, d, p, w, Math.PI, REPLACE);
    }

    public LFO(double f, double d, double p, int w, double dty) {
        this(f, d, p, w, dty, REPLACE);
    }

    public LFO(double f, double d, double p, int w, double dty, int m) {
        frequency = f;
        depth = d;
        phase = p;
        waveform = w;
        duty = dty;
        mode = m;
    }

    public void process(double[][] samples) {
        for (double[] sample : samples) {
            double v = 0;
            switch (waveform) {
                case SINE: v = Math.sin(phase); break;
                case COSINE: v = Math.cos(phase); break;
                case SQUARE: v = (phase > duty) ? 1d : 0d; break;
                case TRIANGLE: v = (phase < Math.PI) ? (phase / Math.PI) : (1d - ((phase - Math.PI) / Math.PI)); break;
                case SAWTOOTH: v = (phase / (Math.PI*2d)); break;
            }
            phase += sampleStep;
            if (phase > (Math.PI * 2d)) {
                phase -= (Math.PI * 2d);
            }

            // Multiply it by the gain factor
            v *= depth;

            // Apply it to the sample
            switch (mode) {
                case REPLACE:
                    sample[Sentence.LEFT] = (sample[Sentence.LEFT] * v);
                    sample[Sentence.RIGHT] = (sample[Sentence.RIGHT] * v);
                    break;
                case ADD:
                    sample[Sentence.LEFT] += (sample[Sentence.LEFT] * v);
                    sample[Sentence.RIGHT] += (sample[Sentence.RIGHT] * v);
                    break;
            }
        }
    }

    public String getName() { return "Low Frequency Oscillator (" + frequency + " Hz, " + (depth * 100d) + "%)"; }

    public ArrayList<Effect> getChildEffects() { return null; }

    public void dump() {
        System.out.println(getName());
    }

    public void init(double sr) {
        sampleRate = sr;

        // Number of samples that make up one cycle of the LFO
        double oneCycle = sampleRate / frequency;

        // Amount to increase each step
        sampleStep = (Math.PI * 2d) / oneCycle;

    }

    public String toString() { return getName(); }

}
