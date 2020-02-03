package uk.co.majenko.audiobookrecorder;

import javax.swing.JPanel;
import java.util.ArrayList;
import java.util.Queue;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Dimension;
import java.awt.Color;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

public class QueueMonitor extends JPanel implements MouseListener {

    ArrayList<WorkerThread> threadList = new ArrayList<WorkerThread>();
    Queue<Runnable> queue;

    public QueueMonitor(Queue<Runnable> q) {
        super();
        queue = q;
        addMouseListener(this);
    }

    public void addThread(WorkerThread t) {
        threadList.add(t);
    }

    public void purgeQueue() {
        Runnable work;
        synchronized (queue) {
            while (queue.size() > 0) {
                work = queue.remove();
                if (work instanceof SentenceJob) {
                    SentenceJob sj = (SentenceJob)work;
                    sj.setDequeued();
                }
            }
            repaint();
        }
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(150 + (24 * threadList.size()), 24);
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

        for (int i = 0; i < threadList.size(); i++) {
            WorkerThread t = threadList.get(i);
            if (t.isRunning()) {
                g.setColor(new Color(50, 200, 0));
            } else {
                g.setColor(new Color(80, 0, 0));
            }
            g.fillOval(i * 24 + 4, 4, 22 - 8, 22 - 8);
        }

        g.setColor(getForeground());
        g.drawString("Queued: " + queue.size(), threadList.size() * 24 + 4, 16);

        if (queue.size() > 0) {
            Icons.close.paintIcon(this, g, size.width - 23, 1);
        }
    }

    @Override
    public void mouseEntered(MouseEvent evt) {   
    }

    @Override
    public void mouseExited(MouseEvent evt) {   
    }

    @Override
    public void mousePressed(MouseEvent evt) {   
    }

    @Override
    public void mouseReleased(MouseEvent evt) {   
    }

    @Override
    public void mouseClicked(MouseEvent evt) {   
        if (queue.size() == 0) return; // No button - ignore it
        Dimension size = getPreferredSize();
        if (evt.getX() > (size.width - 24)) {
            purgeQueue();
        }
    }

}

