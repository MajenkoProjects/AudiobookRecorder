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

    public void process(Sample[] samples) {
        Sample[] savedSamples = new Sample[samples.length];
        for (int i = 0; i < samples.length; i++) {
            savedSamples[i] = new Sample(samples[i].left, samples[i].right);
        }

        for (DelayLineStore d : delayLines) {
            Sample[] subSamples = new Sample[samples.length];
            for (int i = 0; i < samples.length; i++) {
                subSamples[i] = new Sample(savedSamples[i].left, savedSamples[i].right);
            }

            d.process(subSamples);

            for (int i = 0; i < subSamples.length; i++) {
                int off = i + d.getSamples();
                if ((off < samples.length) && (off > 0)) {

                    Sample ns = mix(samples[off], subSamples[i]);
                    samples[off].left = ns.left;
                    samples[off].right = ns.right;
                }
            }
        }
    }

    Sample mix(Sample a, Sample b) {
        Sample out = new Sample(0, 0);

        if ((a.left < 0) && (b.left < 0)) {
            out.left = (a.left + b.left) - (a.left * b.left);
        } else if ((a.left > 0) && (b.left > 0)) {
            out.left = (a.left + b.left) - (a.left * b.left);
        } else {
            out.left = a.left + b.left;
        }

        if ((a.right < 0) && (b.right < 0)) {
            out.right = (a.right + b.right) - (a.right * b.right);
        } else if ((a.right > 0) && (b.right > 0)) {
            out.right = (a.right + b.right) - (a.right * b.right);
        } else {
            out.right = a.right + b.right;
        }

        return out;
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
        }
    }
}
