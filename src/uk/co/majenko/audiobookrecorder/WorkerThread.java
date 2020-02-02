package uk.co.majenko.audiobookrecorder;

import java.util.Queue;

public class WorkerThread extends Thread {
    private static int instance = 0;
    private final Queue<Runnable> queue;
    private final QueueMonitor monitor;

    private boolean running = false;
     
    public WorkerThread(Queue<Runnable> queue, QueueMonitor mon) {
        this.queue = queue;
        monitor = mon;
        setName("Worker Thread " + (instance++));
    }
     
    @Override
    public void run() {
        Debug.d(getName(), "started");
        while ( true ) {
            try {
                Runnable work = null;
 
                synchronized ( queue ) {
                    while ( queue.isEmpty() ) {
                        Debug.d(getName(), "waiting on work");
                        queue.wait();
                    }

                    Debug.d(getName(), "got work");
                     
                    // Get the next work item off of the queue
                    work = queue.remove();
                }
 
                running = true;
                monitor.repaint();
                if (work instanceof SentenceJob) {
                    SentenceJob sj = (SentenceJob)work;
                    sj.setProcessing();
                }
                work.run();
                if (work instanceof SentenceJob) {
                    SentenceJob sj = (SentenceJob)work;
                    sj.setDequeued();
                }
                running = false;
                monitor.repaint();
            }
            catch ( InterruptedException ie ) {
                ie.printStackTrace();
                break;  // Terminate
            }
        }
        Debug.d(getName(), "died");
    }

    public boolean isRunning() {
        return running;
    }
}
