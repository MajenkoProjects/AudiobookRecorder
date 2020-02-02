package uk.co.majenko.audiobookrecorder;

import javax.swing.JPanel;
import java.util.ArrayList;
import java.util.Queue;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Dimension;
import java.awt.Color;

public class QueueMonitor extends JPanel {

    ArrayList<WorkerThread> threadList = new ArrayList<WorkerThread>();
    Queue queue;

    public QueueMonitor(Queue q) {
        super();
        queue = q;
    }

    public void addThread(WorkerThread t) {
        threadList.add(t);
    }

    public void purgeQueue() {
        synchronized (queue) {
            queue.clear();
            repaint();
        }
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(100 + (24 * threadList.size()), 24);
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
    }

}

