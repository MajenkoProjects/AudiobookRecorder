package uk.co.majenko.audiobookrecorder;

import java.util.ArrayList;

public class DelayLine implements Effect {

    ArrayList<DelayLineStore> delayLines;

    boolean wetOnly = false;

    public DelayLine() {
        delayLines = new ArrayList<DelayLineStore>();
    }

    public String getName() {
        return "Delay Line (" + delayLines.size() + " lines)";
    }

    public void process(double[][] samples) {
        double[][] savedSamples = new double[samples.length][2];
        for (int i = 0; i < samples.length; i++) {
            savedSamples[i][Sentence.LEFT] = samples[i][Sentence.LEFT];
            savedSamples[i][Sentence.RIGHT] = samples[i][Sentence.RIGHT];
        }
        if (wetOnly) {
            for (int i = 0; i < samples.length; i++) {
                samples[i][Sentence.LEFT] = 0d;
                samples[i][Sentence.RIGHT] = 0d;
            }
        }

        double[][] subSamples = new double[samples.length][2];
        for (int i = 0; i < samples.length; i++) {
            subSamples[i][Sentence.LEFT] = savedSamples[i][Sentence.LEFT];
            subSamples[i][Sentence.RIGHT] = savedSamples[i][Sentence.RIGHT];
        }
        for (DelayLineStore d : delayLines) {
            for (int i = 0; i < samples.length; i++) {
                subSamples[i][Sentence.LEFT] = savedSamples[i][Sentence.LEFT];
                subSamples[i][Sentence.RIGHT] = savedSamples[i][Sentence.RIGHT];
            }

            d.process(subSamples);

            for (int i = 0; i < subSamples.length; i++) {
                int off = i + d.getSamples();
                if ((off < samples.length) && (off > 0)) {

                    double[] ns = mix(samples[off], subSamples[i]);
                    samples[off][Sentence.LEFT] = ns[Sentence.LEFT];
                    samples[off][Sentence.RIGHT] = ns[Sentence.RIGHT];
                }
            }
        }
    }

    double[] mix(double[] a, double[] b) {
        double[] out = new double[2];

        if ((a[Sentence.LEFT] < 0) && (b[Sentence.LEFT] < 0)) {
            out[Sentence.LEFT] = (a[Sentence.LEFT] + b[Sentence.LEFT]) - (a[Sentence.LEFT] * b[Sentence.LEFT]);
        } else if ((a[Sentence.LEFT] > 0) && (b[Sentence.LEFT] > 0)) {
            out[Sentence.LEFT] = (a[Sentence.LEFT] + b[Sentence.LEFT]) - (a[Sentence.LEFT] * b[Sentence.LEFT]);
        } else {
            out[Sentence.LEFT] = a[Sentence.LEFT] + b[Sentence.LEFT];
        }

        if ((a[Sentence.RIGHT] < 0) && (b[Sentence.RIGHT] < 0)) {
            out[Sentence.RIGHT] = (a[Sentence.RIGHT] + b[Sentence.RIGHT]) - (a[Sentence.RIGHT] * b[Sentence.RIGHT]);
        } else if ((a[Sentence.RIGHT] > 0) && (b[Sentence.RIGHT] > 0)) {
            out[Sentence.RIGHT] = (a[Sentence.RIGHT] + b[Sentence.RIGHT]) - (a[Sentence.RIGHT] * b[Sentence.RIGHT]);
        } else {
            out[Sentence.RIGHT] = a[Sentence.RIGHT] + b[Sentence.RIGHT];
        }

        return out;
    }

    public DelayLineStore addDelayLine(int samples, double gain, double pan) {
        DelayLineStore s = new DelayLineStore(samples, gain, pan);
        delayLines.add(s);
        return s;
    }

    public DelayLineStore addDelayLine(int samples, double gain) {
        DelayLineStore s = new DelayLineStore(samples, gain);
        delayLines.add(s);
        return s;
    }

    public ArrayList<Effect> getChildEffects() {
        return null;
    }

    public String toString() {
        return getName();
    }

    public void dump() {
        System.out.println(toString());
        for (DelayLineStore s : delayLines) {
            s.dump();
        }
    }

    public void init(double sf) {
        for (DelayLineStore s : delayLines) {
            s.init(sf);
        }
    }

    public void setWetOnly(boolean b) {
        wetOnly = b;
    }
}
