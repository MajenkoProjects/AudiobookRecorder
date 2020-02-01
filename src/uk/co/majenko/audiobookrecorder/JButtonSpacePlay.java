package uk.co.majenko.audiobookrecorder;

import javax.swing.JButton;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import java.awt.event.KeyEvent;
import java.awt.event.ActionListener;

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
