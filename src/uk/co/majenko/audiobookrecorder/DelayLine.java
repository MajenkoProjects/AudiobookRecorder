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
        double[][] savedSamples = new double[2][samples[Sentence.LEFT].length];
        for (int i = 0; i < samples[Sentence.LEFT].length; i++) {
            savedSamples[Sentence.LEFT][i] = samples[Sentence.LEFT][i];
            savedSamples[Sentence.RIGHT][i] = samples[Sentence.RIGHT][i];
        }
        if (wetOnly) {
            for (int i = 0; i < samples[Sentence.LEFT].length; i++) {
                samples[Sentence.LEFT][i] = 0d;
                samples[Sentence.RIGHT][i] = 0d;
            }
        }

        double[][] subSamples = new double[2][samples[Sentence.LEFT].length];
        for (int i = 0; i < samples[Sentence.LEFT].length; i++) {
            subSamples[Sentence.LEFT][i] = savedSamples[Sentence.LEFT][i];
            subSamples[Sentence.RIGHT][i] = savedSamples[Sentence.RIGHT][i];
        }
        for (DelayLineStore d : delayLines) {
            for (int i = 0; i < samples[Sentence.LEFT].length; i++) {
                subSamples[Sentence.LEFT][i] = savedSamples[Sentence.LEFT][i];
                subSamples[Sentence.RIGHT][i] = savedSamples[Sentence.RIGHT][i];
            }

            d.process(subSamples);

            for (int i = 0; i < subSamples[Sentence.LEFT].length; i++) {
                int off = i + d.getSamples();
                if ((off < samples[Sentence.LEFT].length) && (off > 0)) {
                    samples[Sentence.LEFT][off] = Utils.mix(samples[Sentence.LEFT][off], subSamples[Sentence.LEFT][i]);
                    samples[Sentence.RIGHT][off] = Utils.mix(samples[Sentence.RIGHT][off], subSamples[Sentence.RIGHT][i]);
                }
            }
        }
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

    public void init(double sf) {
        for (DelayLineStore s : delayLines) {
            s.init(sf);
        }
    }

    public void setWetOnly(boolean b) {
        wetOnly = b;
    }
}
