package uk.co.majenko.audiobookrecorder;

import javax.swing.JLabel;
import java.awt.Dimension;

public class JLabelFixedWidth extends JLabel {
    int width = 0;
    int height = 0;
    Dimension size;
    public JLabelFixedWidth(int w, String txt) {
        super(txt);
        JLabel t = new JLabel(txt);
        size = t.getPreferredSize();
        size.width = w;
    }

    public JLabelFixedWidth(int w) {
        super();
        JLabel t = new JLabel("nothing");
        size = t.getPreferredSize();
        size.width = w;
    }

    @Override
    public Dimension getPreferredSize() {
        return size;
    }

    @Override
    public Dimension getMaximumSize() {
        return size;
    }

    @Override
    public Dimension getMinimumSize() {
        return size;
    }
}
