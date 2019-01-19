package uk.co.majenko.audiobookrecorder;

import java.util.ArrayList;

public class LFO implements Effect {
    
    double phase;
    double frequency;
    double depth;
    double sampleRate;
    double sampleStep;

    public LFO(double f, double d) {
        frequency = f;
        depth = d;
        phase = 0;
    }

    public LFO(double f, double d, double p) {
        frequency = f;
        depth = d;
        phase = p;
    }

    public double process(double sample) {
        double v = Math.sin(phase);
        phase += sampleStep;
        if (phase > (Math.PI * 2d)) {
            phase -= (Math.PI * 2d);
        }

//        // Make it between 0 and 1.
//        v = 1d + v;
//        v /= 2d;

        // Multiply it by the gain factor
        v *= depth;

        // Apply it to the sample
        sample += (sample * v);

        return sample;
    }

    public String getName() { return "Low Frequency Oscillator (" + frequency + " Hz, " + (depth * 100d) + "%)"; }

    public ArrayList<Effect> getChildEffects() { return null; }

    public void dump() {
        System.out.println(getName());
    }

    public void init(double sr) {
        sampleRate = sr;

        // Number of samples that make up one cycle of the LFO
        double oneCycle = sampleRate / frequency;

        // Amount to increase each step
        sampleStep = (Math.PI * 2d) / oneCycle;

    }

    public String toString() { return getName(); }

}
