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


    public Equaliser() {
        super();

        channels = new EqualiserChannel[31];

        setLayout(new BorderLayout());

        JPanel inner = new JPanel();

        inner.setLayout(new FlowLayout());

        for (int i = 0; i < 31; i++) {
            channels[i] = new EqualiserChannel();
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

        add(smooth, BorderLayout.SOUTH);
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
}
