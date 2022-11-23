import io.swagger.client.ApiClient;
import io.swagger.client.ApiException;
import io.swagger.client.ApiResponse;
import io.swagger.client.api.SkiersApi;

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
        int successfulPosts = 0;
        int failedPosts = 0;
        String basePath = "http://35.160.218.179:8080/server_war/";
//        String basePath = "http://localhost:8080/server_war_exploded/";
        SkiersApi apiInstance = new SkiersApi();
        ApiClient client = apiInstance.getApiClient();
        client.setBasePath(basePath);
        for (int i = 0; i < NUMBER_OF_REQUESTS; i++) {

            // maximum of 5 tries
            boolean success = false;
            int numTries = 0;

            while (!success && numTries < MAX_RETRIES) {
                try {
                    if (Main.q.peek() == null) {
                        break;
                    }
                    LiftRideEvent liftRideEvent = Main.q.take();
                    ApiResponse<Void> response = apiInstance.writeNewLiftRideWithHttpInfo(liftRideEvent.getLiftRide(), liftRideEvent.getResortID(), liftRideEvent.getSeasonID(),
                            liftRideEvent.getDayID(), liftRideEvent.getSkierID());
                    System.out.println(response.getStatusCode());
                    if (response.getStatusCode() >= 400) {
                        sleepThread(numTries++);
                        continue;
                    }
                    successfulPosts++;
                } catch (ApiException e) {
                    System.out.println(e.getCode());
                    System.err.println("Exception thrown while calling SkierApi for writeNewLiftRide");
                    System.err.println(e.getMessage());
                    failedPosts++;
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
            this.sharedResults.incrementSuccessfulPost(successfulPosts);
            this.sharedResults.incrementFailedPost(failedPosts);

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
