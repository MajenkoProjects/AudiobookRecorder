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
        for (int i = 0; i < samples[Sentence.LEFT].length; i++) {
            if (samples[Sentence.LEFT][i] > clip) samples[Sentence.LEFT][i] = clip;
            if (samples[Sentence.LEFT][i] < -clip) samples[Sentence.LEFT][i] = -clip;
            if (samples[Sentence.RIGHT][i] > clip) samples[Sentence.RIGHT][i] = clip;
            if (samples[Sentence.RIGHT][i] < -clip) samples[Sentence.RIGHT][i] = -clip;
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

    public void init(double sf) {
    }
}
