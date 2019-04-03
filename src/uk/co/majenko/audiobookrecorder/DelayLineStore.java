package uk.co.majenko.audiobookrecorder;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.ArrayList;

public class DelayLineStore {
    ArrayBlockingQueue<Double> fifo;
    double gain;
    int numSamples;

    ArrayList<Effect> effects;

    public DelayLineStore(int s, double g) {
        fifo = new ArrayBlockingQueue<Double>(s);
        for (int i = 0; i < s; i++) {
            fifo.add(0d);
        }
        numSamples = s;
        gain = g;
        effects = new ArrayList<Effect>();
    }

    public double pass(double s) {
        try {
            for (Effect e : effects) {
                s = e.process(s);
            }
            double v = s * gain;
            double t = fifo.poll();
            fifo.add(v);
            return t;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0d;
    }

    public void setSamples(int s) {
        fifo = new ArrayBlockingQueue<Double>(s);
        for (int i = 0; i < s; i++) {
            fifo.add(0d);
        }
        numSamples = s;
    }

    public void setGain(double g) {
        gain = g;
    }

    public int getSamples() {
        return numSamples;
    }

    public double getGain() {
        return gain;
    }

    public void purge() {
        fifo.clear();
        for (int i = 0; i < numSamples; i++) {
            fifo.add(0d);
        }
    }

    public void addEffect(Effect e) {
        effects.add(e);
    }

    public void init(double sf) {
        for (Effect e : effects) {
            e.init(sf);
        }
        purge();
    }

    public void dump() {
        System.out.println("    Samples: " + numSamples + ", gain: " + gain);
        for (Effect e : effects) {
            e.dump();
        }
    }
}
