package uk.co.majenko.audiobookrecorder;

import java.util.ArrayList;

public class DelayLine implements Effect {

    ArrayList<DelayLineStore> delayLines;

    public DelayLine() {
        delayLines = new ArrayList<DelayLineStore>();
    }

    public String getName() {
        return "Delay Line (" + delayLines.size() + " lines)";
    }

    public double process(double sample) {
        double s = sample;
        for (DelayLineStore d : delayLines) {
            double echo = d.pass(sample);
            s = mix(s, echo);
        }
        return s;
    }

    double mix(double a, double b) {
        if ((a < 0) && (b < 0)) {
            return (a + b) - (a * b);
        }  
        if ((a > 0) && (b > 0)) {
            return (a + b) - (a * b);
        }
        return a + b;
    }

    public DelayLineStore addDelayLine(int samples, double gain) {
        DelayLineStore s = new DelayLineStore(samples, gain);
        delayLines.add(s);
        return s;
    }

    public ArrayList<Effect> getChildEffects() {
        return null;
    }

    public String toString() {
        return getName();
    }

    public void dump() {
        System.out.println(toString());
        for (DelayLineStore s : delayLines) {
            s.dump();
        }
    }

    public void init(double sf) {
        for (DelayLineStore s : delayLines) {
            s.init(sf);
            s.purge();
        }
    }


}
