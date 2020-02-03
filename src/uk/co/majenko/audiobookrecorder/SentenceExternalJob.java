package uk.co.majenko.audiobookrecorder;

import java.lang.Runnable;

public class SentenceExternalJob extends SentenceJob {
    protected int command;

    public SentenceExternalJob(Sentence s, int c) {
        super(s);
        command = c;
    }

    @Override
    public void run() {
        sentence.runExternalProcessor(command);
    }
}
