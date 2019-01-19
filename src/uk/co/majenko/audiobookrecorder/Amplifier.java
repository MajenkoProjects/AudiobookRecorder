package uk.co.majenko.audiobookrecorder;

import java.util.ArrayList;

public class Amplifier implements Effect {
    double gain;
    public Amplifier() {
        gain = 1.0d;
    }
    public Amplifier(double g) {
        gain = g;
    }

    public String getName() {
        return "Amplifier (" + gain + ")";
    }

    public ArrayList<Effect> getChildEffects() {
        return null;
    }

    public double process(double sample) {
        return sample * gain;
    }

    public double getGain() {
        return gain;
    }
   
    public void setGain(double g) {
        gain = g;
    }

    public String toString() {
        return getName();
    }

    public void dump() {
        System.out.println(toString());
    }

    public void init(double sf) {
    }
}
