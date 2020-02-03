package uk.co.majenko.audiobookrecorder;

import javax.swing.JPanel;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Dimension;
import java.awt.Color;

public class NoiseFloor extends JPanel {

    int noiseFloor = 0;

    public NoiseFloor() {
        super();
        noiseFloor = 0;
    }

    public void setNoiseFloor(int n) {
        noiseFloor = n;
        repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(128, 24);
    }

    @Override
    public Dimension getMinimumSize() {
        return getPreferredSize();
    }

    @Override
    public Dimension getMaximumSize() {
        return getPreferredSize();
    }

    @Override
    public void paintComponent(Graphics g) {
        Rectangle size = g.getClipBounds();
        g.setColor(getBackground());
        g.fillRect(0, 0, size.width - 1, size.height - 1);
        g.setColor(new Color(10, 10, 10));
        g.drawRect(0, 0, size.width - 1, size.height - 1);
        g.setFont(getFont());

        g.setColor(getForeground());
        g.drawString("Noise Floor: " + noiseFloor + "dB", 6, 16);
    }
}

