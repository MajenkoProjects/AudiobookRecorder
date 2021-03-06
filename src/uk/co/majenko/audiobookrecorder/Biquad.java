package uk.co.majenko.audiobookrecorder;

import java.util.ArrayList;

public class Biquad implements Effect {
    public static final int Lowpass = 0;
    public static final int Highpass = 1;
    public static final int Bandpass = 2;
    public static final int Notch = 3;
    public static final int Peak = 4;
    public static final int Lowshelf = 5;
    public static final int Highshelf = 6;

    int type;
    double a0, a1, a2, b1, b2;
    double Fc, Q, peakGain;
    double lz1, lz2;
    double rz1, rz2;
    double sampleFrequency;

    public Biquad() {
        Debug.trace();
        type = Lowpass;
        a0 = 1.0d;
        a1 = 0.0d;
        a2 = 0.0d;
        b1 = 0.0d;
        b2 = 0.0d;
        Fc = 440d;
        Q = 0.707d;
        peakGain = 0.0d;
        lz1 = 0.0d;
        lz2 = 0.0d;
        rz1 = 0.0d;
        rz2 = 0.0d;
        sampleFrequency = 44100d;
    }

    public Biquad(int type, double Fc, double Q, double peakGainDB) {
        Debug.trace();
        setBiquad(type, Fc, Q, peakGainDB);
        lz1 = 0.0;
        lz2 = 0.0;
        rz1 = 0.0;
        rz2 = 0.0;
        sampleFrequency = 44100d;
    }

    public void setType(int typei) {
        Debug.trace();
        type = typei;
        calcBiquad();
    }

    public void setQ(double Qi) {
        Debug.trace();
        Q = Qi;
        calcBiquad();
    }

    public void setFc(double Fci) {
        Debug.trace();
        Fc = Fci;
        calcBiquad();
    }

    public void setPeakGain(double peakGainDB) {
        Debug.trace();
        peakGain = peakGainDB;
        calcBiquad();
    }

    public void setBiquad(int typei, double Fci, double Qi, double peakGainDB) {
        Debug.trace();
        type = typei;
        Q = Qi;
        Fc = Fci;
        setPeakGain(peakGainDB);
    }

    // Special single channel version for wave profile processing
    public void process(double[] samples) {
        Debug.trace();
        lz1 = 0d;
        lz2 = 0d;
        for (int i = 0; i < samples.length; i++) {
            double lout = samples[i] * a0 + lz1;

            lz1 = samples[i] * a1 + lz2 - b1 * lout;
            lz2 = samples[i] * a2 - b2 * lout;

            samples[i] = lout;
        }
    }

    public void process(double[][] samples) {
        Debug.trace();
        lz1 = 0d;
        lz2 = 0d;
        rz1 = 0d;
        rz2 = 0d;
        for (int i = 0; i < samples[Sentence.LEFT].length; i++) {
            double lout = samples[Sentence.LEFT][i] * a0 + lz1;

            lz1 = samples[Sentence.LEFT][i] * a1 + lz2 - b1 * lout;
            lz2 = samples[Sentence.LEFT][i] * a2 - b2 * lout;

            double rout = samples[Sentence.RIGHT][i] * a0 + rz1;

            rz1 = samples[Sentence.RIGHT][i] * a1 + rz2 - b1 * rout;
            rz2 = samples[Sentence.RIGHT][i] * a2 - b2 * rout;

            samples[Sentence.LEFT][i] = lout;
            samples[Sentence.RIGHT][i] = rout;
        }
    }
    
    public void init(double sf) {
        Debug.trace();
        sampleFrequency = sf;
        lz1 = 0d;
        lz2 = 0d;
        rz1 = 0d;
        rz2 = 0d;
        calcBiquad();
    }
    
    void calcBiquad() {
        Debug.trace();

        double norm;
        double V = Math.pow(10, Math.abs(peakGain) / 20.0);
        double K = Math.tan(Math.PI * (Fc/sampleFrequency));
        switch (type) {
            case Lowpass:
                norm = 1d / (1d + K / Q + K * K);
                a0 = K * K * norm;
                a1 = 2d * a0;
                a2 = a0;
                b1 = 2d * (K * K - 1d) * norm;
                b2 = (1d - K / Q + K * K) * norm;
                break;
                
            case Highpass:
                norm = 1d / (1d + K / Q + K * K);
                a0 = 1d * norm;
                a1 = -2d * a0;
                a2 = a0;
                b1 = 2d * (K * K - 1d) * norm;
                b2 = (1d - K / Q + K * K) * norm;
                break;
                
            case Bandpass:
                norm = 1d / (1d + K / Q + K * K);
                a0 = K / Q * norm;
                a1 = 0d;
                a2 = -a0;
                b1 = 2d * (K * K - 1d) * norm;
                b2 = (1d - K / Q + K * K) * norm;
                break;
                
            case Notch:
                norm = 1d / (1d + K / Q + K * K);
                a0 = (1d + K * K) * norm;
                a1 = 2d * (K * K - 1d) * norm;
                a2 = a0;
                b1 = a1;
                b2 = (1d - K / Q + K * K) * norm;
                break;
                
            case Peak:
                if (peakGain >= 0d) {    // boost
                    norm = 1d / (1d + 1d/Q * K + K * K);
                    a0 = (1d + V/Q * K + K * K) * norm;
                    a1 = 2d * (K * K - 1d) * norm;
                    a2 = (1d - V/Q * K + K * K) * norm;
                    b1 = a1;
                    b2 = (1d - 1d/Q * K + K * K) * norm;
                }
                else {    // cut
                    norm = 1d / (1d + V/Q * K + K * K);
                    a0 = (1d + 1d/Q * K + K * K) * norm;
                    a1 = 2d * (K * K - 1d) * norm;
                    a2 = (1d - 1d/Q * K + K * K) * norm;
                    b1 = a1;
                    b2 = (1d - V/Q * K + K * K) * norm;
                }
                break;
            case Lowshelf:
                if (peakGain >= 0) {    // boost
                    norm = 1d / (1 + Math.sqrt(2d) * K + K * K);
                    a0 = (1d + Math.sqrt(2d*V) * K + V * K * K) * norm;
                    a1 = 2d * (V * K * K - 1d) * norm;
                    a2 = (1d - Math.sqrt(2d*V) * K + V * K * K) * norm;
                    b1 = 2d * (K * K - 1d) * norm;
                    b2 = (1d - Math.sqrt(2d) * K + K * K) * norm;
                }
                else {    // cut
                    norm = 1d / (1 + Math.sqrt(2d*V) * K + V * K * K);
                    a0 = (1d + Math.sqrt(2d) * K + K * K) * norm;
                    a1 = 2d * (K * K - 1d) * norm;
                    a2 = (1d - Math.sqrt(2d) * K + K * K) * norm;
                    b1 = 2d * (V * K * K - 1d) * norm;
                    b2 = (1d - Math.sqrt(2d*V) * K + V * K * K) * norm;
                }
                break;
            case Highshelf:
                if (peakGain >= 0d) {    // boost
                    norm = 1d / (1d + Math.sqrt(2d) * K + K * K);
                    a0 = (V + Math.sqrt(2d*V) * K + K * K) * norm;
                    a1 = 2d * (K * K - V) * norm;
                    a2 = (V - Math.sqrt(2d*V) * K + K * K) * norm;
                    b1 = 2d * (K * K - 1d) * norm;
                    b2 = (1d - Math.sqrt(2d) * K + K * K) * norm;
                }
                else {    // cut
                    norm = 1d / (V + Math.sqrt(2d*V) * K + K * K);
                    a0 = (1d + Math.sqrt(2d) * K + K * K) * norm;
                    a1 = 2d * (K * K - 1d) * norm;
                    a2 = (1d - Math.sqrt(2d) * K + K * K) * norm;
                    b1 = 2d * (K * K - V) * norm;
                    b2 = (V - Math.sqrt(2d*V) * K + K * K) * norm;
                }
                break;
        }
        
        return;
    }

    public String getName() {
        Debug.trace();
        String n = "Biquad Filter (";
        switch (type) {
            case Lowpass: n += "Lowpass"; break;
            case Highpass: n += "Highpass"; break;
            case Bandpass: n += "Bandpass"; break;
            case Notch: n += "Notch"; break;
            case Peak: n += "Peak"; break;
            case Lowshelf: n += "Lowshelf"; break;
            case Highshelf: n += "Highshelf"; break;
        }
        n += ", Fc=";
        n += Fc;
        n += ", Q=";
        n += Q;
        n += ", Gain=";
        n += peakGain;
        n += "dB)";
        return n;
    }

    public ArrayList<Effect> getChildEffects() {
        Debug.trace();
        return null;
    }

    public String toString() {
        Debug.trace();
        return getName();
    }

    public void dump() {
        Debug.trace();
        System.out.println(toString());
    }
}
