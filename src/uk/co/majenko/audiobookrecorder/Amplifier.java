package uk.co.majenko.audiobookrecorder;

import java.util.ArrayList;

public class Amplifier implements Effect {
    double gain;
    public Amplifier() {
        Debug.trace();
        gain = 1.0d;
    }
    public Amplifier(double g) {
        Debug.trace();
        gain = g;
    }

    public String getName() {
        Debug.trace();
        return "Amplifier (" + gain + ")";
    }

    public ArrayList<Effect> getChildEffects() {
        Debug.trace();
        return null;
    }

    public void process(double[][] samples) {
        Debug.trace();
        for (int i = 0; i < samples[Sentence.LEFT].length; i++) {
            samples[Sentence.LEFT][i] *= gain;
            samples[Sentence.RIGHT][i] *= gain;
        }
    }

    public double getGain() {
        Debug.trace();
        return gain;
    }
   
    public void setGain(double g) {
        Debug.trace();
        gain = g;
    }

    public String toString() {
        Debug.trace();
        return getName();
    }

    public void dump() {
        Debug.trace();
        System.out.println(toString());
    }

    public void init(double sf) {
        Debug.trace();
    }
}
