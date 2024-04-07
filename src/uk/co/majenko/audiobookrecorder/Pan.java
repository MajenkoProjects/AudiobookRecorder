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
        for (int i = 0; i < samples[Sentence.LEFT].length; i++) {
            if (pan < 0) {
                double p = 1 + pan;
                samples[Sentence.RIGHT][i] *= p;
            } else {
                double p = 1 - pan;
                samples[Sentence.LEFT][i] *= p;
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

    public void init(double sf) {
    }
}
