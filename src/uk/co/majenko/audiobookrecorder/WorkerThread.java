package uk.co.majenko.audiobookrecorder;

import java.util.Queue;

public class WorkerThread extends Thread {
    private static int instance = 0;
    private final Queue<Runnable> queue;
     
    public WorkerThread(Queue<Runnable> queue) {
        this.queue = queue;
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
 
                // Process the work item
                work.run();
            }
            catch ( InterruptedException ie ) {
                ie.printStackTrace();
                break;  // Terminate
            }
        }
        Debug.d(getName(), "died");
    }
}
