package uk.co.majenko.audiobookrecorder;

import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import javax.swing.JToggleButton;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import javax.swing.ImageIcon;

public class JToggleButtonSpacePlay extends JToggleButton {
    public JToggleButtonSpacePlay(ImageIcon i, String tt, ActionListener al) {
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
