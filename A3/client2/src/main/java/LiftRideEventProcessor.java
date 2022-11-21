import io.swagger.client.ApiClient;
import io.swagger.client.ApiException;
import io.swagger.client.ApiResponse;
import io.swagger.client.api.SkiersApi;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;

public class LiftRideEventProcessor extends Thread {
    private static final Integer MAX_RETRIES = 5;
    private final BlockingQueue<LiftRideEvent> sharedQueue;
    private CountDownLatch latch;
    private CountDownLatch overallLatch;

    private SharedResults sharedResults;

    private static final Integer NUMBER_OF_REQUESTS = 1000;

    public LiftRideEventProcessor(BlockingQueue sharedQueue, CountDownLatch latch, CountDownLatch overallLatch, SharedResults sharedResults) {
        this.sharedQueue = sharedQueue;
        this.latch = latch;
        this.overallLatch = overallLatch;
        this.sharedResults = sharedResults;
    }

    public void run() {
        Timestamp startTime;
        Timestamp endTime;
        int responseCode;
        List<String> fileData = new ArrayList<>();
        int successfulPosts = 0;
        int failedPosts = 0;
        String basePath = "http://ds-hw2-load-balancer-1559362f763ddc4f.elb.us-west-2.amazonaws.com:8080/server_war/";
        SkiersApi apiInstance = new SkiersApi();
        ApiClient client = apiInstance.getApiClient();
        client.setBasePath(basePath);
        for (int i = 0; i < NUMBER_OF_REQUESTS; i++) {

            // maximum of 5 tries
            boolean success = false;
            int numTries = 0;

            while (!success && numTries < MAX_RETRIES) {
                startTime = new Timestamp(System.currentTimeMillis());
                try {
                    if (Main.q.peek() == null) {
                        break;
                    }
                    LiftRideEvent liftRideEvent = Main.q.take();
                    ApiResponse<Void> response = apiInstance.writeNewLiftRideWithHttpInfo(liftRideEvent.getLiftRide(), liftRideEvent.getResortID(), liftRideEvent.getSeasonID(),
                            liftRideEvent.getDayID(), liftRideEvent.getSkierID());
                    endTime = new Timestamp(System.currentTimeMillis());
                    if (response.getStatusCode() >= 400) {
                        sleepThread(numTries++);
                        continue;
                    }
                    successfulPosts++;
                    responseCode = 201;
                } catch (ApiException e) {
                    endTime = new Timestamp(System.currentTimeMillis());
                    System.out.println(e.getCode());
                    System.err.println("Exception thrown while calling SkierApi for writeNewLiftRide");
                    System.err.println(e.getMessage());
                    responseCode = e.getCode();
                    failedPosts++;
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                long latency = endTime.getTime() - startTime.getTime();
                String fileLine = startTime + ",POST," + latency +
                        "," + responseCode + "\n";
                fileData.add(fileLine);
            }
        }
            this.sharedResults.incrementSuccessfulPost(successfulPosts);
            this.sharedResults.incrementFailedPost(failedPosts);
            this.sharedResults.addNewResults(fileData);
            try {
                if (this.latch != null) {
                    this.latch.countDown();
                }
                this.overallLatch.countDown();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    private void sleepThread(Integer numTries) {
        try {
            Thread.sleep(2^numTries);
        } catch (InterruptedException ex){
            ex.printStackTrace();
        }
    }
}
