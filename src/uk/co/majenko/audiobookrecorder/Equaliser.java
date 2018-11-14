package uk.co.majenko.audiobookrecorder;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.io.*;
import java.nio.file.*;
import javax.swing.tree.*;
import javax.sound.sampled.*;
import davaguine.jeq.core.IIRControls;

public class Equaliser extends JPanel {

    EqualiserChannel channels[];
    String name;

    static final double[] frequencies = {
        20d, 25d, 31.5d, 40d, 50d, 63d, 80d, 100d, 125d, 160d, 200d,
        250d, 315d, 400d, 500d, 630d, 800d, 1000d, 1250d, 1600d, 2000d,
        2500d, 3150d, 4000d, 5000d, 6300d, 8000d, 10000d, 12500d, 16000d,
        20000d
    };

    public Equaliser(String n) {
        super();

        name = n;

        channels = new EqualiserChannel[31];

        setLayout(new BorderLayout());

        JPanel inner = new JPanel();

        inner.setLayout(new FlowLayout());

        for (int i = 0; i < 31; i++) {
            channels[i] = new EqualiserChannel(frequencies[i]);
            inner.add(channels[i]);
        }

        add(inner, BorderLayout.CENTER);
        

        JButton smooth = new JButton("Smooth curve");
        smooth.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Float ave[] = new Float[31];
                for (int i = 1; i < 30; i++) {
                    ave[i] = (channels[i-1].getValue() + channels[i].getValue() + channels[i+1].getValue()) / 3.0f;
                }

                for (int i = 1; i < 30; i++) {
                    channels[i].setValue(ave[i]);
                }
            }
        });
        JButton def = new JButton("Set as default");
        def.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                for (int i = 0; i < 31; i++) {
                    Options.set("audio.eq." + i, channels[i].getValue());
                }
            }
        });
        JButton load = new JButton("Load from default");
        load.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                for (int i = 0; i < 31; i++) {
                    channels[i].setValue(Options.getFloat("audio.eq." + i));
                }
            }
        });

        JPanel buttons = new JPanel();
        buttons.setLayout(new FlowLayout());
        buttons.add(smooth);
        buttons.add(def);
        buttons.add(load);

        add(buttons, BorderLayout.SOUTH);
    }

    public float getChannel(int c) {
        return channels[c].getValue();
    }

    public void setChannel(int c, float v) {
        channels[c].setValue(v);
    }

    public void apply(IIRControls c, int chans) {
        for (int i = 0; i < 31; i++) {
            c.setBandDbValue(i, 0, channels[i].getValue());
            if (chans == 2) {
                c.setBandDbValue(i, 1, channels[i].getValue());
            }
        }
    }

    public String getName() {
        return name;
    }
}
