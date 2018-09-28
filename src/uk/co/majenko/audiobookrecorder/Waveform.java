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

    int leftAltMarker = 0;
    int rightAltMarker = 0;

    int dragging = 0;

    int step = 1;

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
            step = num / w;
            if (step == 0) return;

            for (int n = 0; n < w; n++) {
                int hcnt = 0;
                long have = 0;
                int hmax = 0;

                int lcnt = 0;
                int lave = 0;
                int lmax = 0;

                for (int o = 0; o < step; o++) {
                    int sample = samples[(n * step) + o];
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

                hmax /= scale;
                lmax /= scale;
                have /= scale;
                lave /= scale;

                g.setColor(new Color(0, 0, 100));
                g.drawLine(n, h/2 + lmax, n, h/2 - hmax);
                g.setColor(new Color(0, 0, 200));
                g.drawLine(n, h/2 + (int)lave, n, h/2 - (int)have);
            }

            g.setColor(new Color(255, 0, 0, 32));
            g.fillRect(0, 0, leftAltMarker/step, h);
            g.fillRect(rightAltMarker/step, 0, w , h);

            g.setColor(new Color(255, 0, 0));
            g.drawLine(leftAltMarker/step, 0, leftAltMarker/step, h);
            g.drawLine(rightAltMarker/step, 0, rightAltMarker/step, h);

            g.setColor(new Color(255, 255, 0));
            g.drawLine(leftMarker/step, 0, leftMarker/step, h);
            g.drawLine(rightMarker/step, 0, rightMarker/step, h);

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
        if ((x >= (leftMarker/step) - 2) && (x <= (leftMarker/step) + 2)) {
            leftMarkerSaved = leftMarker;
            rightMarkerSaved = rightMarker;
            dragging = 1;
            return;
        }
        if ((x >= (rightMarker/step) - 2) && (x <= (rightMarker/step) + 2)) {
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
        if ((x >= (leftMarker/step) - 2) && (x <= (leftMarker/step) + 2)) {
            setCursor(Cursor.getPredefinedCursor(Cursor.W_RESIZE_CURSOR));
            return;
        }
        if ((x >= (rightMarker/step) - 2) && (x <= (rightMarker/step) + 2)) {
            setCursor(Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR));
            return;
        }

        setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));

    }

    public void mouseDragged(MouseEvent e) {
        if (dragging == 0) return;
        int x = e.getX();

        if (dragging == 1) {
            leftMarker = x * step;
            if (leftMarker > rightMarker) {
                leftMarker  = rightMarker;
            }
        } else if (dragging == 2) {
            rightMarker = x * step;
            if (rightMarker < leftMarker) {
                rightMarker = leftMarker;
            }
        }

        repaint();
        
    }
}
