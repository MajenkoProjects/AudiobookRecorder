package uk.co.majenko.audiobookrecorder;

import java.util.TimerTask;
import java.util.Timer;
import javax.swing.JPanel;
import javax.swing.JComponent;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Color;
import java.awt.Dimension;

public class FlashPanel extends JPanel {

    boolean flash = false;
    boolean col = false;

    Timer ticker;
    
    public FlashPanel() {
        super();
        ticker = new Timer(true);
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

        col = true;

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
            g.setColor(Color.GREEN);
        }
        Dimension d = getSize();
        g.fillRect(0, 0, d.width, d.height);
    }

}
