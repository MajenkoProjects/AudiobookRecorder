package uk.co.majenko.audiobookrecorder;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;

public class JButtonSpacePlay extends JButton {
    public JButtonSpacePlay(ImageIcon i, String tt, ActionListener al) {
        super(i);
        getInputMap(JComponent.WHEN_FOCUSED).put(
            KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), 
            "startPlayback"
        );

        setToolTipText(tt);
        addActionListener(al);
        setFocusPainted(false);
        setFocusable(false);
    }
}
