import com.squareup.okhttp.Interceptor;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;

public class Main {

    private static Integer TOTAL_REQUESTS = 200000;
    private static Integer START_PHASE_THREADS =  32;
    private static Integer SECOND_PHASE_THREADS = 168;

    private static Integer MILLISECONDS_TO_SECONDS_CONVERTER = 1000;
    private static String path = "results.csv";

    protected static BlockingQueue<LiftRideEvent> q = new LinkedBlockingQueue<>();

    public static void main(String[] args) throws InterruptedException, IOException {

        SharedResults results = new SharedResults();

        Integer totalThreads = START_PHASE_THREADS + SECOND_PHASE_THREADS;
        CountDownLatch latch = new CountDownLatch(1);
        CountDownLatch overallLatch = new CountDownLatch(totalThreads);

        new LiftRideEventGenerator(q, TOTAL_REQUESTS).start();
        Phase phase1 = new Phase(START_PHASE_THREADS, latch, overallLatch, results, q);

        // Phase 2
        Phase phase2 = new Phase(SECOND_PHASE_THREADS, null, overallLatch, results, q);

        try {
            long wallTime = runPhases(phase1, phase2, latch, overallLatch);
            int throughput = (int) Math.round(TOTAL_REQUESTS/(wallTime/MILLISECONDS_TO_SECONDS_CONVERTER));
            System.out.println("Number of successful posts: " + results.getSuccessfulPosts());
            System.out.println("Number of failed posts: " + results.getFailedPosts());
            System.out.println("Wall time: " + wallTime + "ms");
            System.out.println("throughput: " + throughput);
            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(path));
            for (String result: results.getFileLines() ){
                bufferedWriter.write(result);
            }
            bufferedWriter.close();
        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
        }
        DataProcessor processor = new DataProcessor(path);
        processor.readFromFile();
        System.out.println("mean response time: " + processor.getMean()) ;
        System.out.println("median response time: " + processor.getMedian());
        System.out.println("p99: " + processor.getP99());
        System.out.println("max response time: " + processor.getMax());
    }
    private static long runPhases(Phase phase1, Phase phase2, CountDownLatch latch, CountDownLatch overallLatch) throws InterruptedException {
        Timestamp startTime = new Timestamp(System.currentTimeMillis());
        phase1.run();
        latch.await();
        System.out.println("phase 1 and starting phase 2");
        phase2.run();
        overallLatch.await();
        System.out.println("phase 2 completed");
        Timestamp endTime = new Timestamp(System.currentTimeMillis());
        return endTime.getTime()-startTime.getTime();
    }
}
