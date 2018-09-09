package uk.co.majenko.audiobookrecorder;

import javax.swing.*;
import java.awt.*;
import java.util.*;

public class FlashPanel extends JPanel {

    boolean flash = false;
    boolean col = false;

    java.util.Timer ticker;
    
    public FlashPanel() {
        super();
        ticker = new java.util.Timer(true);
        ticker.scheduleAtFixedRate(new TimerTask() {
            public void run() {
                if (flash) {
                    col = !col;
                    repaint();
                }
            }
        }, 0, 500);
    }

    public void setFlash(boolean f) {
        flash = f;

        for (Component o : getComponents()) {
            ((JComponent)o).setVisible(!f);
        }
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (flash == false) {
            super.paintComponent(g);
            return;
        }
        if (col) {
            g.setColor(Color.RED);
        } else {
            g.setColor(Color.BLACK);
        }
        Dimension d = getSize();
        g.fillRect(0, 0, d.width, d.height);
    }

}
