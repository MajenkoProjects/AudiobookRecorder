package uk.co.majenko.audiobookrecorder;

import java.util.ArrayList;

public class Pan implements Effect {
    double pan;
    public Pan() {
        pan = 0.0d;
    }
    public Pan(double p) {
        pan = p;
    }

    public String getName() {
        return "Pan (" + pan + ")";
    }

    public ArrayList<Effect> getChildEffects() {
        return null;
    }

    public void process(double[][] samples) {
        for (double[] sample : samples) {
            if (pan < 0) {
                double p = 1 + pan;
                sample[Sentence.RIGHT] *= p;
            } else {
                double p = 1 - pan;
                sample[Sentence.LEFT] *= p;
            }
        }
    }

    public double getPan() {
        return pan;
    }
   
    public void setPan(double p) {
        pan = p;
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
