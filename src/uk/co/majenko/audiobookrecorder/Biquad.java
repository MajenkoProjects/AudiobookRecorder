package uk.co.majenko.audiobookrecorder;

//  Biquad.java
//
//  Created by Nigel Redmon on 11/24/12
//  EarLevel Engineering: earlevel.com
//  Copyright 2012 Nigel Redmon
//  Translated to Java 2019 Majenko Technologies
//
//  For a complete explanation of the Biquad code:
//  http://www.earlevel.com/main/2012/11/26/biquad-c-source-code/
//
//  License:
//
//  This source code is provided as is, without warranty.
//  You may copy and distribute verbatim copies of this document.
//  You may modify and use this source code to create binary code
//  for your own purposes, free or commercial.
//

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
        setBiquad(type, Fc, Q, peakGainDB);
        lz1 = 0.0;
        lz2 = 0.0;
        rz1 = 0.0;
        rz2 = 0.0;
        sampleFrequency = 44100d;
    }

    public void setType(int typei) {
        type = typei;
        calcBiquad();
    }

    public void setQ(double Qi) {
        Q = Qi;
        calcBiquad();
    }

    public void setFc(double Fci) {
        Fc = Fci;
        calcBiquad();
    }

    public void setPeakGain(double peakGainDB) {
        peakGain = peakGainDB;
        calcBiquad();
    }

    public void setBiquad(int typei, double Fci, double Qi, double peakGainDB) {
        type = typei;
        Q = Qi;
        Fc = Fci;
        setPeakGain(peakGainDB);
    }

    public void process(Sample[] samples) {
        lz1 = 0d;
        lz2 = 0d;
        rz1 = 0d;
        rz2 = 0d;
        for (Sample in : samples) {
            double lout = in.left * a0 + lz1;

            lz1 = in.left * a1 + lz2 - b1 * lout;
            lz2 = in.left * a2 - b2 * lout;

            double rout = in.right * a0 + rz1;

            rz1 = in.right * a1 + rz2 - b1 * rout;
            rz2 = in.right * a2 - b2 * rout;

            in.left = lout;
            in.right = rout;
        }
    }
    
    public void init(double sf) {
        sampleFrequency = sf;
        lz1 = 0d;
        lz2 = 0d;
        rz1 = 0d;
        rz2 = 0d;
        calcBiquad();
    }
    
    void calcBiquad() {

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
        return null;
    }

    public String toString() {
        return getName();
    }

    public void dump() {
        System.out.println(toString());
    }
}
