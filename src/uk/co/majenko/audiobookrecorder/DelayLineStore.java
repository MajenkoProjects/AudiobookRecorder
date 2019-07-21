package uk.co.majenko.audiobookrecorder;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.ArrayList;

public class DelayLineStore {
    double gain;
    int numSamples;

    ArrayList<Effect> effects;

    public DelayLineStore(int s, double g) {
        numSamples = s;
        gain = g;
        effects = new ArrayList<Effect>();
    }

    public void process(Sample[] samples) {
        for (Effect e : effects) {
            e.process(samples);
        }

        for (Sample sample : samples) {
            sample.left *= gain;
            sample.right *= gain;
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
