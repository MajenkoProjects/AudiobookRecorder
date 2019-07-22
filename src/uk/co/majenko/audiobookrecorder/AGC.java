package uk.co.majenko.audiobookrecorder;

import java.util.ArrayList;

public class AGC implements Effect {
    double limit;
    double gain;
    double ceiling;
    double decay;
    double attack;

    public AGC(double c, double a, double d, double l) {
        ceiling = c;
        attack = a;
        decay = d;
        limit = l;
        gain = 1d;
    }

    public String getName() {
        return "AGC (Ceiling = " + ceiling + " attack = " + attack + " decay = " + decay + " limit = " + limit;
    }

    public String toString() {
        return getName();
    }

    public void process(double[][] samples) {
        gain = 1d;
        for (int i = 0; i < samples.length; i++) {
            double absSampleLeft = Math.abs(samples[i][Sentence.LEFT]) * gain;
            double absSampleRight = Math.abs(samples[i][Sentence.RIGHT]) * gain;

            if (absSampleLeft > ceiling) {
                gain -= attack;
                if (gain < 0) gain = 0;
            }
            
            if (absSampleRight > ceiling) {
                gain -= attack;
                if (gain < 0) gain = 0;
            }
        
            if ((absSampleLeft < ceiling) && (absSampleRight < ceiling)) {
                gain += decay;
                if (gain > limit) {
                    gain = limit;
                }
            }

            samples[i][Sentence.LEFT] *= gain;
            samples[i][Sentence.RIGHT] *= gain;
        }
    }

    public void init(double sr) {
        gain = 1d;
    }

    public void dump() {
        System.out.println(toString());
    }

    public ArrayList<Effect> getChildEffects() {
        return null;
    }
}
