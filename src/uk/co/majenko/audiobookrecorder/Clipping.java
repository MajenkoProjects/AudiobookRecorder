package uk.co.majenko.audiobookrecorder;

import java.util.ArrayList;

public class Clipping implements Effect {
    double clip;
    public Clipping() {
        clip = 1.0d;
    }
    public Clipping(double g) {
        clip = g;
    }

    public String getName() {
        return "Clipping (" + clip + ")";
    }

    public ArrayList<Effect> getChildEffects() {
        return null;
    }

    public double process(double sample) {
        if (sample > clip) return clip;
        if (sample < -clip) return -clip;
        return sample;
    }

    public double getClip() {
        return clip;
    }
   
    public void setClip(double g) {
        clip = g;
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
