import com.squareup.okhttp.Interceptor;

import java.sql.Timestamp;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;

public class Main {

    // create 3 threads
    private static Integer START_UP_POST_REQUESTS = 1000;
    private static Integer SECOND_PHASE_REQUESTS = 1800;
    private static Integer THIRD_PHASE_REQUESTS = 1500;
    private static Integer START_PHASE_THREADS =  32;
    private static Integer SECOND_PHASE_THREADS = 55;
    private static Integer THIRD_PHASE_THREADS = 50;

    private static Integer MILLISECONDS_TO_SECONDS_CONVERTER = 1000;

    private static BlockingQueue<LiftRideEvent> q;

    public static void main(String[] args) {
        SharedResults results = new SharedResults();
        Integer totalRequests = START_UP_POST_REQUESTS + SECOND_PHASE_REQUESTS + THIRD_PHASE_REQUESTS;

        Integer totalThreads = START_PHASE_THREADS + SECOND_PHASE_THREADS + THIRD_PHASE_THREADS;
        CountDownLatch overallLatch = new CountDownLatch(totalThreads);

        // Initial phase
        LiftRideEventGenerator eventGenerator = new LiftRideEventGenerator(q, START_UP_POST_REQUESTS);
        Phase phase1 = new Phase(START_PHASE_THREADS, SECOND_PHASE_THREADS + THIRD_PHASE_THREADS, overallLatch, results, eventGenerator.getSharedQueue());

        // Phase 2
        LiftRideEventGenerator eventGenerator1 = new LiftRideEventGenerator(q, SECOND_PHASE_REQUESTS);
        Phase phase2 = new Phase(SECOND_PHASE_THREADS, THIRD_PHASE_THREADS, overallLatch, results, eventGenerator1.getSharedQueue());

        // Phase 3
        LiftRideEventGenerator eventGenerator2 = new LiftRideEventGenerator(q, THIRD_PHASE_REQUESTS);
        Phase phase3 = new Phase(THIRD_PHASE_THREADS, 0, overallLatch, results, eventGenerator2.getSharedQueue());

        try {
            long wallTime = runPhases(phase1, phase2, phase3, overallLatch);
            int throughput = (int) Math.round(totalRequests/(wallTime/MILLISECONDS_TO_SECONDS_CONVERTER));
            System.out.println("Number of successful posts: " + results.getSuccessfulPosts());
            System.out.println("Number of failed posts: " + results.getFailedPosts());
            System.out.println("Wall time: " + wallTime + "ms");
            System.out.println("TotalRequests: "+ totalRequests);
            System.out.println("throughput: " + throughput);

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    private static long runPhases(Phase phase1, Phase phase2, Phase phase3, CountDownLatch overallLatch) throws InterruptedException {
        Timestamp startTime = new Timestamp(System.currentTimeMillis());
        phase1.run();
        phase2.run();
        phase3.run();
        overallLatch.await();
        Timestamp endTime = new Timestamp(System.currentTimeMillis());
        return endTime.getTime()-startTime.getTime();
    }
}
