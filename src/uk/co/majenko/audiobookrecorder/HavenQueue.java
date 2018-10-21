package uk.co.majenko.audiobookrecorder;

import java.util.*;
import java.util.concurrent.*;
import javax.swing.*;
import java.awt.*;
import java.util.Timer;

public class HavenQueue extends JPanel {
    ConcurrentLinkedQueue<Sentence> sentenceList = new ConcurrentLinkedQueue<Sentence>();

    Timer timer = new Timer();

    Sentence currentSentence = null;

    JLabel count;

    public HavenQueue() {
        timer.schedule(new TimerTask() { public void run() { processQueue(); }}, 30000);
        count = new JLabel("Haven queue: 0");
        setLayout(new BorderLayout());
        add(count, BorderLayout.CENTER);

        count.setOpaque(false);
        setOpaque(false);
        count.setForeground(Color.WHITE);
    }

    public void processQueue() {

        count.setText("Haven queue: " + sentenceList.size());

        if (currentSentence == null) {
            // Grab a new sentence to process.
            currentSentence = sentenceList.poll();

            if (currentSentence != null) {
                if (!currentSentence.postHavenData()) { // Failed. Add to the end of the queue and wait a bit
                    submit(currentSentence);
                    currentSentence = null;
                    timer.schedule(new TimerTask() { public void run() { processQueue(); }}, 30000);
                    return;
                }
                timer.schedule(new TimerTask() { public void run() { processQueue(); }}, 5000);
                return;
            }

            timer.schedule(new TimerTask() { public void run() { processQueue(); }}, 5000);
            return;
        }

        if (currentSentence != null) {
            currentSentence.processPendingHaven();
            int status = currentSentence.getHavenStatus();
            switch (status) {
                case 0: // Um... not running...?
                    currentSentence = null;
                    timer.schedule(new TimerTask() { public void run() { processQueue(); }}, 30000);
                    return;
                case 1: // Still processing...
                    timer.schedule(new TimerTask() { public void run() { processQueue(); }}, 5000);
                    return;
                case 2: // Finished
                    currentSentence = null;
                    timer.schedule(new TimerTask() { public void run() { processQueue(); }}, 30000);
                    return;
                case 3: // Failed
                    currentSentence = null;
                    timer.schedule(new TimerTask() { public void run() { processQueue(); }}, 30000);
                    return;
            }
        }

        timer.schedule(new TimerTask() { public void run() { processQueue(); }}, 30000);
    }

    public void submit(Sentence s) {
        s.setOverrideText("[queued...]");
        AudiobookRecorder.window.bookTreeModel.reload(s);
        sentenceList.add(s);
        count.setText("Haven queue: " + sentenceList.size());
    }
}
