import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;

public class Phase {
    private int numThreads;
    private int numWaitThreads;
    private CountDownLatch overAllLatch;
    private SharedResults results;

    private BlockingQueue<LiftRideEvent> q;

    public Phase(int numThreads, int numWaitThreads, CountDownLatch overAllLatch, SharedResults results, BlockingQueue<LiftRideEvent> q) {
        this.numThreads = numThreads;
        this.numWaitThreads = numWaitThreads;
        this.overAllLatch = overAllLatch;
        this.results = results;
        this.q = q;
    }

    public void run() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(this.numWaitThreads);
        for (int i = 0; i < this.numThreads; i++){
            Thread thread = new LiftRideEventProcessor(q, this.overAllLatch, latch, results);
            thread.start();
        }

    }
}
