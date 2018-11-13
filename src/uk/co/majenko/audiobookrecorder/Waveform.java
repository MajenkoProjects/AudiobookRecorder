package uk.co.majenko.audiobookrecorder;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.io.*;
import javax.sound.sampled.*;

public class Waveform extends JPanel implements MouseListener, MouseMotionListener {

    int[] samples = null;

    int leftMarker = 0;
    int rightMarker = 0;
    int leftMarkerSaved = 0;
    int rightMarkerSaved = 0;

    int playMarker = 0;

    int leftAltMarker = 0;
    int rightAltMarker = 0;

    int dragging = 0;

    int step = 1;

    int zoomFactor = 1;
    int offsetFactor = 0;
    int offset = 0;

    ArrayList<MarkerDragListener> markerDragListeners;

    public Waveform() {
        super();
        addMouseListener(this);
        addMouseMotionListener(this);
        markerDragListeners = new ArrayList<MarkerDragListener>();
    }

    public void addMarkerDragListener(MarkerDragListener l) {
        if (markerDragListeners.indexOf(l) == -1) {
            markerDragListeners.add(l);
        }
    }

    public void paintComponent(Graphics g) {
        Dimension size = getSize();

        int w = size.width;
        int h = size.height;

        g.setColor(Color.BLACK);
        g.fillRect(0, 0, w, h);

        g.setColor(Color.GREEN);
        g.drawRect(0, 0, w, h);
        g.drawLine(0, h/2, w, h/2);

        g.setColor(new Color(0, 150, 0));
        for (int x = 0; x < w; x += w/10) {
            g.drawLine(x, 0, x, h);
        }

        for (int x = 0; x < w; x += 4) {
            g.drawLine(x, h/4, x, h/4);
            g.drawLine(x, h/4*3, x, h/4*3);
        }

        int scale = 32768/(h/2);

        if (samples != null) {

            int num = samples.length;
            step = num / zoomFactor / w;
            if (step == 0) return;

            double off = offsetFactor / 1000d;
            int ns = (num - (w * step));
            offset = (int)(ns * off);

            for (int n = 0; n < w; n++) {
                int hcnt = 0;
                long have = 0;
                int hmax = 0;

                int lcnt = 0;
                int lave = 0;
                int lmax = 0;

                for (int o = 0; o < step; o++) {
                    int sample = samples[offset + (n * step) + o];
                    if (sample >= 0) {
                        have += sample;
                        hcnt++;
                        if (sample > hmax) hmax = sample;
                    } else {
                        sample = Math.abs(sample);
                        lave += sample;
                        lcnt++;
                        if (sample > lmax) lmax = sample;
                    }
                }

                if (hcnt > 0) have /= hcnt;
                if (lcnt > 0) lave /= lcnt;

                boolean clip = false;

                if (lmax > 32000) { // -3dB == 23198?
                    clip = true;
                }

                if (hmax > 32000) { // -3dB
                    clip = true;
                }

                hmax /= scale;
                lmax /= scale;
                have /= scale;
                lave /= scale;

                if (clip) {
                    g.setColor(new Color(200, 20, 0));
                } else {
                    g.setColor(new Color(0, 20, 200));
                }
                g.drawLine(n, h/2 + lmax, n, h/2 - hmax);
                g.setColor(new Color(0, 100, 255));
                g.drawLine(n, h/2 + (int)lave, n, h/2 - (int)have);
            }

            g.setColor(new Color(255, 0, 0, 32));
            g.fillRect(0, 0, (leftAltMarker - offset)/step, h);
            g.fillRect((rightAltMarker - offset)/step, 0, (num - rightAltMarker) / step , h);

            g.setColor(new Color(255, 0, 0));
            g.drawLine((leftAltMarker - offset)/step, 0, (leftAltMarker - offset)/step, h);
            g.drawLine((rightAltMarker - offset)/step, 0, (rightAltMarker - offset)/step, h);

            g.setColor(new Color(255, 255, 0));
            g.drawLine((leftMarker - offset)/step, 0, (leftMarker - offset)/step, h);
            g.drawLine((rightMarker - offset)/step, 0, (rightMarker - offset)/step, h);

            g.setColor(new Color(0, 255, 255));
            for (int i = 0; i < h; i += 2) {
                g.drawLine((playMarker - offset) / step, i, (playMarker - offset) / step, i);
            }
        }
    }

    public void setAltMarkers(int l, int r) {
        leftAltMarker = l;
        rightAltMarker = r;
        repaint();
    }

    public void setMarkers(int l, int r) {
        leftMarker = l;
        rightMarker = r;
        repaint();
    }

    public void setLeftAltMarker(int l) {
        leftAltMarker = l;
        repaint();
    }

    public void setRightAltMarker(int r) {
        rightAltMarker = r;
        repaint();
    }

    public void setLeftMarker(int l) {
        leftMarker = l;
        repaint();
    }

    public void setRightMarker(int r) {
        rightMarker = r;
        repaint();
    }

    public void clearData() {
        samples = null;
        repaint();
    }

    public void setData(int[] s) {
        samples = s;
        playMarker = 0;
        repaint();
    }

    public void mouseExited(MouseEvent e) {
        if (dragging != 0) {
            leftMarker = leftMarkerSaved;
            rightMarker = rightMarkerSaved;
            repaint();
        }
        dragging = 0;
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mousePressed(MouseEvent e) {
        int x = e.getX();
        if ((x >= ((leftMarker - offset)/step) - 10) && (x <= ((leftMarker - offset)/step) + 10)) {
            leftMarkerSaved = leftMarker;
            rightMarkerSaved = rightMarker;
            dragging = 1;
            return;
        }
        if ((x >= ((rightMarker - offset)/step) - 10) && (x <= ((rightMarker - offset)/step) + 10)) {
            rightMarkerSaved = rightMarker;
            leftMarkerSaved = leftMarker;
            dragging = 2;
            return;
        }
    }

    public void mouseReleased(MouseEvent e) {
        if (dragging == 1) {
            MarkerDragEvent evt = new MarkerDragEvent(this, leftMarker);
            for (MarkerDragListener l : markerDragListeners) {
                l.leftMarkerMoved(evt);
            }
        } else if (dragging == 2) {
            MarkerDragEvent evt = new MarkerDragEvent(this, rightMarker);
            for (MarkerDragListener l : markerDragListeners) {
                l.rightMarkerMoved(evt);
            }
        }
        dragging = 0;
    }

    public void mouseClicked(MouseEvent e) {
    }

    public void mouseMoved(MouseEvent e) {
        int x = e.getX();
        if ((x >= ((leftMarker - offset)/step) - 10) && (x <= ((leftMarker - offset)/step) + 10)) {
            setCursor(Cursor.getPredefinedCursor(Cursor.W_RESIZE_CURSOR));
            return;
        }
        if ((x >= ((rightMarker - offset)/step) - 10) && (x <= ((rightMarker - offset)/step) + 10)) {
            setCursor(Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR));
            return;
        }

        setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));

    }

    public void mouseDragged(MouseEvent e) {
        if (dragging == 0) return;
        int x = e.getX();

        if (dragging == 1) {
            leftMarker = (x * step) + offset;
            if (leftMarker > rightMarker) {
                leftMarker  = rightMarker;
            }
        } else if (dragging == 2) {
            rightMarker = (x * step) + offset;
            if (rightMarker < leftMarker) {
                rightMarker = leftMarker;
            }
        }

        repaint();
        
    }

    public void setOffset(int permil) {
        offsetFactor = permil;
        if (offsetFactor < 0) offsetFactor = 0;
        if (offsetFactor > 1000) offsetFactor = 1000;
        repaint();
    }

    public void increaseZoom() {
        zoomFactor *= 2;
        if (zoomFactor > 64) zoomFactor = 64;
        repaint();
    }

    public void decreaseZoom() {
        zoomFactor /= 2;
        if (zoomFactor < 1) zoomFactor = 1;
        repaint();
    }

    public void setPlayMarker(int m) {
        playMarker = leftAltMarker + m;
        repaint();
    }
}
