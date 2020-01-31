package uk.co.majenko.audiobookrecorder;

import java.util.ArrayList;

public class AGC implements Effect {
    double limit;
    double gain;
    double ceiling;
    double decay;
    double attack;

    public AGC(double c, double a, double d, double l) {
        Debug.trace();
        ceiling = c;
        attack = a;
        decay = d;
        limit = l;
        gain = 1d;
    }

    public String getName() {
        Debug.trace();
        return "AGC (Ceiling = " + ceiling + " attack = " + attack + " decay = " + decay + " limit = " + limit;
    }

    public String toString() {
        Debug.trace();
        return getName();
    }

    public void process(double[][] samples) {
        Debug.trace();
        gain = 1d;
        for (int i = 0; i < samples[Sentence.LEFT].length; i++) {
            double absSampleLeft = Math.abs(samples[Sentence.LEFT][i]) * gain;
            double absSampleRight = Math.abs(samples[Sentence.RIGHT][i]) * gain;

            double factor = 0.0d;

            if (absSampleLeft > ceiling) {
                factor = -attack;
            }
            
            if (absSampleRight > ceiling) {
                factor = -attack;
            }
        
            if ((absSampleLeft < ceiling) && (absSampleRight < ceiling)) {
                factor = decay;
            }

            gain += factor;
            if (gain > limit) gain = limit;
            if (gain < 0) gain = 0;

            samples[Sentence.LEFT][i] *= gain;
            samples[Sentence.RIGHT][i] *= gain;
        }
    }

    public void init(double sr) {
        Debug.trace();
        gain = 1d;
    }

    public void dump() {
        Debug.trace();
        System.out.println(toString());
    }

    public ArrayList<Effect> getChildEffects() {
        Debug.trace();
        return null;
    }
}
