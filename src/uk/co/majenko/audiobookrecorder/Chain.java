package uk.co.majenko.audiobookrecorder;

import java.util.ArrayList;

public class Chain implements Effect {
    String target;

    public Chain(String t) {
        target = t;
    }

    public Chain() {
        target = null;
    }

    public void process(double[][] samples) {
        if (target != null) {
            Effect t = AudiobookRecorder.window.effects.get(target);
            if (t != null) {
                t.process(samples);
            }
        }
    }

    public void setTarget(String t) {
        target = t;
    }

    public String getTarget() {
        return target;
    }

    public String toString() {
        return "Chain to " + target;
    }

    public void dump() {
        System.out.println(toString());
    }

    public void init(double sf) {
        if (target != null) {
            Effect t = AudiobookRecorder.window.effects.get(target);
            if (t != null) {
                t.init(sf);
            }
        }
    }

    public ArrayList<Effect> getChildEffects() {
        return null;
    }

    public String getName() { 
        return toString();
    }

}
