package uk.co.majenko.audiobookrecorder;

//  Biquad.h
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

public class Biquad {
    public static final int bq_type_lowpass = 0;
    public static final int bq_type_highpass = 1;
    public static final int bq_type_bandpass = 2;
    public static final int bq_type_notch = 3;
    public static final int bq_type_peak = 4;
    public static final int bq_type_lowshelf = 5;
    public static final int bq_type_highshelf = 6;

    int type;
    double a0, a1, a2, b1, b2;
    double Fc, Q, peakGain;
    double z1, z2;

    public Biquad() {
        type = bq_type_lowpass;
        a0 = 1.0d;
        a1 = 0.0d;
        a2 = 0.0d;
        b1 = 0.0d;
        b2 = 0.0d;
        Fc = 0.50d;
        Q = 0.707d;
        peakGain = 0.0d;
        z1 = 0.0d;
        z2 = 0.0d;
    }

    public Biquad(int type, double Fc, double Q, double peakGainDB) {
        setBiquad(type, Fc, Q, peakGainDB);
        z1 = 0.0;
        z2 = 0.0;
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

    public float process(float in) {
        double out = in * a0 + z1;
        z1 = in * a1 + z2 - b1 * out;
        z2 = in * a2 - b2 * out;
        return (float)out;
    }
    
    void calcBiquad() {

        double norm;
        double V = Math.pow(10, Math.abs(peakGain) / 20.0);
        double K = Math.tan(Math.PI * Fc);
        switch (type) {
            case bq_type_lowpass:
                norm = 1d / (1d + K / Q + K * K);
                a0 = K * K * norm;
                a1 = 2d * a0;
                a2 = a0;
                b1 = 2d * (K * K - 1d) * norm;
                b2 = (1d - K / Q + K * K) * norm;
                break;
                
            case bq_type_highpass:
                norm = 1d / (1d + K / Q + K * K);
                a0 = 1d * norm;
                a1 = -2d * a0;
                a2 = a0;
                b1 = 2d * (K * K - 1d) * norm;
                b2 = (1d - K / Q + K * K) * norm;
                break;
                
            case bq_type_bandpass:
                norm = 1d / (1d + K / Q + K * K);
                a0 = K / Q * norm;
                a1 = 0d;
                a2 = -a0;
                b1 = 2d * (K * K - 1d) * norm;
                b2 = (1d - K / Q + K * K) * norm;
                break;
                
            case bq_type_notch:
                norm = 1d / (1d + K / Q + K * K);
                a0 = (1d + K * K) * norm;
                a1 = 2d * (K * K - 1d) * norm;
                a2 = a0;
                b1 = a1;
                b2 = (1d - K / Q + K * K) * norm;
                break;
                
            case bq_type_peak:
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
            case bq_type_lowshelf:
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
            case bq_type_highshelf:
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
}
