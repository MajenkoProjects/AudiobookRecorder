package uk.co.majenko.audiobookrecorder;

import javax.swing.*;
import java.awt.*;

public class AboutPanel extends JPanel {

    public AboutPanel() {
        Debug.trace();
        setLayout(new BorderLayout());
        JLabel icon = new JLabel(Icons.appIcon);
        add(icon, BorderLayout.WEST);

        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.PAGE_AXIS));

        p.add(Box.createVerticalGlue());

        JLabel l1 = new JLabel("AudiobookRecorder");
        JLabel l2 = new JLabel("Version " + AudiobookRecorder.config.getProperty("version"));
        JLabel l3 = new JLabel("(c) 2020 Majenko Technologies");

        l1.setAlignmentX(JComponent.CENTER_ALIGNMENT);
        l2.setAlignmentX(JComponent.CENTER_ALIGNMENT);
        l3.setAlignmentX(JComponent.CENTER_ALIGNMENT);

        p.add(l1);
        p.add(Box.createVerticalGlue());
        p.add(l2);
        p.add(l3);

        p.add(Box.createVerticalGlue());
        add(p, BorderLayout.CENTER);
    }
}
