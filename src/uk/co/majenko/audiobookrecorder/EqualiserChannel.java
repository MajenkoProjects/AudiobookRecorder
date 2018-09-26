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

public class EqualiserChannel extends JPanel {

    float value;
    JSlider slider;
    JTextField textbox;

    public EqualiserChannel() {
        super();

        value = 0;

        slider = new JSlider(-120, 120, 0);
        textbox = new JTextField();

        setLayout(new BorderLayout());

        slider.setOrientation(SwingConstants.VERTICAL);
        add(slider, BorderLayout.CENTER);
        textbox = new JTextField("0.0");
        add(textbox, BorderLayout.SOUTH);

        textbox.setPreferredSize(new Dimension(40, 20));
        slider.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                value = (float)slider.getValue() / 10.0f;
                textbox.setText(String.format("%.1f", value));
            }
        });

        textbox.addFocusListener(new FocusListener() {
            public void focusGained(FocusEvent e) {
                textbox.selectAll();
            }

            public void focusLost(FocusEvent e) {
                value = Utils.s2f(textbox.getText());
                if (value < -12f) value = -12f;
                if (value > 12f) value = 12f;

                slider.setValue((int)(value * 10));
                textbox.setText(String.format("%.1f", value));
            }
        });

        textbox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                value = Utils.s2f(textbox.getText());
                if (value < -12f) value = -12f;
                if (value > 12f) value = 12f;

                slider.setValue((int)(value * 10));
                textbox.setText(String.format("%.1f", value));
            }
        });
    }

    public float getValue() {
        return value;
    }

    public void setValue(float v) {
        value = v;
        slider.setValue((int)(value * 10));
        textbox.setText(String.format("%.1f", value));
    }
}