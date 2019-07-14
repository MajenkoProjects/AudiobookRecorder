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

    public double process(double sample) {
        double absSample = Math.abs(sample) * gain;

        if (absSample > ceiling) {
            gain -= attack;
            if (gain < 0) gain = 0;
        }
        
        if (absSample < ceiling) {
            gain += decay;
            if (gain > limit) {
                gain = limit;
            }
        }

        sample *= gain;

        return sample;
    }

    public void init(double sr) {
    }

    public void dump() {
        System.out.println(toString());
    }

    public ArrayList<Effect> getChildEffects() {
        return null;
    }
}
