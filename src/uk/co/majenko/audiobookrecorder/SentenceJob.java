package uk.co.majenko.audiobookrecorder;

import java.lang.Runnable;

public abstract class SentenceJob implements Runnable {
    protected Sentence sentence;
    
    public SentenceJob(Sentence s) {
        sentence = s;
    }

    public void setQueued() {
        sentence.setQueued();
    }

    public void setDequeued() {
        sentence.setDequeued();
    }

    public void setProcessing() {
        sentence.setProcessing();
    }
    
    public abstract void run();
}
