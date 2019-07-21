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

    public void process(Sample[] samples) {
        for (Sample sample : samples) {
            if (sample.left > clip) sample.left = clip;
            if (sample.left < -clip) sample.left = -clip;
            if (sample.right > clip) sample.right = clip;
            if (sample.right < -clip) sample.right = -clip;
        }
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
