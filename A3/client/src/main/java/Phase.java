import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;

public class Phase {
    private int numThreads;
    private CountDownLatch latch;
    private CountDownLatch overAllLatch;
    private SharedResults results;

    private BlockingQueue<LiftRideEvent> q;

    public Phase(int numThreads, CountDownLatch latch, CountDownLatch overAllLatch, SharedResults results, BlockingQueue<LiftRideEvent> q) {
        this.numThreads = numThreads;
        this.latch = latch;
        this.overAllLatch = overAllLatch;
        this.results = results;
        this.q = q;
    }

    public void run() {
        for (int i = 0; i < this.numThreads; i++){
            Thread thread = new LiftRideEventProcessor(q, this.latch, this.overAllLatch, results);
            thread.start();
        }
    }
}
