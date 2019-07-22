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

    public void process(double[][] samples) {
        for (double[] sample : samples) {
            if (sample[Sentence.LEFT] > clip) sample[Sentence.LEFT] = clip;
            if (sample[Sentence.LEFT] < -clip) sample[Sentence.LEFT] = -clip;
            if (sample[Sentence.RIGHT] > clip) sample[Sentence.RIGHT] = clip;
            if (sample[Sentence.RIGHT] < -clip) sample[Sentence.RIGHT] = -clip;
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
