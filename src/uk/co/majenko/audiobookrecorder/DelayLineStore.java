package uk.co.majenko.audiobookrecorder;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.ArrayList;

public class DelayLineStore {
    double gain;
    int numSamples;
    double pan;

    ArrayList<Effect> effects;

    public DelayLineStore(int s, double g, double p) {
        numSamples = s;
        gain = g;
        pan = p;
        effects = new ArrayList<Effect>();
    }

    public DelayLineStore(int s, double g) {
        numSamples = s;
        gain = g;
        pan = 0d;
        effects = new ArrayList<Effect>();
    }

    public void process(double[][] samples) {
        for (Effect e : effects) {
            e.process(samples);
        }

        for (int i = 0; i < samples[Sentence.LEFT].length; i++) {
            samples[Sentence.LEFT][i] *= gain;
            samples[Sentence.RIGHT][i] *= gain;

            if (pan < 0) {
                double p = 1 + pan;
                samples[Sentence.RIGHT][i] *= p;
            } else {
                double p = 1 - pan;
                samples[Sentence.LEFT][i] *= p;
            }
        }
    }

    public void setSamples(int s) {
        numSamples = s;
    }

    public void setGain(double g) {
        gain = g;
    }

    public int getSamples() {
        return numSamples;
    }

    public double getGain() {
        return gain;
    }

    public void addEffect(Effect e) {
        effects.add(e);
    }

    public void init(double sf) {
        for (Effect e : effects) {
            e.init(sf);
        }
    }

    public void dump() {
        System.out.println("    Samples: " + numSamples + ", gain: " + gain);
        for (Effect e : effects) {
            e.dump();
        }
    }
}
