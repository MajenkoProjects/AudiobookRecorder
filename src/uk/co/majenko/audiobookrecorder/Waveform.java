package uk.co.majenko.audiobookrecorder;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.io.*;
import javax.sound.sampled.*;

public class Waveform extends JPanel implements MouseListener, MouseMotionListener {

    double[][] samples = null;

    int leftMarker = 0;
    int rightMarker = 0;
    int leftMarkerSaved = 0;
    int rightMarkerSaved = 0;

    int playMarker = 0;

    int leftAltMarker = 0;
    int rightAltMarker = 0;

    int cutEntry = 0;
    int cutExit = 0;
    boolean displayCut = false;
    boolean displaySplit = false;

    int dragging = 0;

    int step = 1;

    int zoomFactor = 1;
    int offsetFactor = 0;
    int offset = 0;

    String loadedId = null;

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

        int h2 = h/2;
        int db3 = (int) (h2 * 0.708);

        for (int x = 0; x < w; x += 4) {
            g.drawLine(x, h2 + db3, x, h2 + db3);
            g.drawLine(x, h2 - db3, x, h2 - db3);
        }

        double scale = (h/2);

        if (samples != null) {

            int num = samples[Sentence.LEFT].length;
            step = num / zoomFactor / w;
            if (step == 0) return;

            double off = offsetFactor / 1000d;
            int ns = (num - (w * step));
            offset = (int)(ns * off);

            for (int n = 0; n < w; n++) {
                int hcnt = 0;
                double have = 0;
                double hmax = 0;

                int lcnt = 0;
                double lave = 0;
                double lmax = 0;

                for (int o = 0; o < step; o++) {
                    if (offset + (n * step) + o >= samples[Sentence.LEFT].length) break;
                    double sample = (samples[Sentence.LEFT][offset + (n * step) + o] + samples[Sentence.RIGHT][offset + (n * step) + o]) / 2d;
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

                if (lmax > 0.708) { // -3dB == 23198?
                    clip = true;
                }

                if (hmax > 0.708) { // -3dB
                    clip = true;
                }

                hmax *= scale;
                lmax *= scale;
                have *= scale;
                lave *= scale;

                if (clip) {
                    g.setColor(new Color(200, 20, 0));
                } else {
                    g.setColor(new Color(0, 20, 200));
                }
                g.drawLine(n, (int)(h/2 + lmax), n, (int)(h/2 - hmax));
                g.setColor(new Color(0, 100, 255));
                g.drawLine(n, (int)(h/2 + lave), n, (int)(h/2 - have));
            }

            g.setColor(new Color(255, 0, 0, 64));
            g.fillRect(0, 0, (leftAltMarker - offset)/step, h);
            g.fillRect((rightAltMarker - offset)/step, 0, (num - rightAltMarker) / step , h);

            g.setColor(new Color(255, 0, 0));
            g.drawLine((leftAltMarker - offset)/step, 0, (leftAltMarker - offset)/step, h);
            g.drawLine((rightAltMarker - offset)/step, 0, (rightAltMarker - offset)/step, h);

            g.setColor(new Color(255, 255, 0));
            g.drawLine((leftMarker - offset)/step, 0, (leftMarker - offset)/step, h);
            g.drawLine((rightMarker - offset)/step, 0, (rightMarker - offset)/step, h);

            if (displayCut || displaySplit) {
                g.setColor(new Color(0, 255, 255));
                g.drawLine((cutEntry - offset)/step, 0, (cutEntry - offset)/step, h);
            }

            if (displayCut) {
                g.setColor(new Color(0, 255, 255));
                g.drawLine((cutExit - offset)/step, 0, (cutExit - offset)/step, h);
                
                g.setColor(new Color(0, 255, 255, 80));
                g.fillRect((cutEntry - offset)/step, 0, ((cutExit - offset) - (cutEntry - offset))/step , h);
            }

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

    public void setData(double[][] s) {
        samples = s;
        playMarker = 0;
        displayCut = false;
        displaySplit = false;
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
        if (displayCut || displaySplit) {
            if ((x >= ((cutEntry - offset)/step) - 10) && (x <= ((cutEntry - offset)/step) + 10)) {
                dragging = 3;
                return;
            }
        }
        if (displayCut) {
            if ((x >= ((cutExit - offset)/step) - 10) && (x <= ((cutExit - offset)/step) + 10)) {
                dragging = 4;
                return;
            }
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
        if (displayCut || displaySplit) {
            if ((x >= ((cutEntry - offset)/step) - 10) && (x <= ((cutEntry - offset)/step) + 10)) {
                setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
                return;
            }
        } 

        if (displayCut) {
            if ((x >= ((cutExit - offset)/step) - 10) && (x <= ((cutExit - offset)/step) + 10)) {
                setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
                return;
            }
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
        } else if (dragging == 3) {
            cutEntry = (x * step) + offset;
            if (displayCut) {
                if (cutEntry > cutExit) {
                    cutEntry = cutExit;
                }
            }
        } else if (dragging == 4) {
            cutExit = (x * step) + offset;
            if (cutExit < cutEntry) {
                cutExit = cutEntry;
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

    public void setDisplayCut(boolean c) {
        displayCut = c;
        displaySplit = false;
        if (displayCut) {
            int d = rightMarker - leftMarker;
            cutEntry = leftMarker + (d / 3);
            cutExit = leftMarker + (d / 3 * 2);
        }
        repaint();
    }

    public void setDisplaySplit(boolean c) {
        displayCut = false;
        displaySplit = c;
        if (displaySplit) {
            int d = rightMarker - leftMarker;
            cutEntry = leftMarker + (d / 2);
        }
        repaint();
    }

    public int getCutStart() {
        return cutEntry;
    }

    public int getCutEnd() {
        return cutExit;
    }

    public void setId(String id) {
        loadedId = id;
    }

    public String getId() {
        return loadedId;
    }
}
