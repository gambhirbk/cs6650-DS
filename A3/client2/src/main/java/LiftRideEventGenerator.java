import io.swagger.client.model.LiftRide;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadLocalRandom;

public class LiftRideEventGenerator extends Thread {

    private static Integer startSkierID = 1;
    private static Integer endSkierID = 100000;
    private static Integer startLiftID = 1;
    private static Integer endLiftID= 40;
    private static Integer startResortID = 1;
    private static Integer endResortID = 10;
    private static Integer startTime = 1;
    private static Integer endTime = 360;
    private static final String SEASON_ID = "2022";

    private static Integer startDay = 1;

    private static Integer endDay = 365;
//    private static String DAY_ID = "7";

    private LiftRideEvent liftRideEvent;

    private final BlockingQueue<LiftRideEvent> sharedQueue;
    private int numPostRequests;

    public LiftRideEventGenerator(BlockingQueue sharedQueue, int numPostRequests) {
        this.sharedQueue = sharedQueue;
        this.numPostRequests = numPostRequests;
    }

    @Override
    public void run() {
        for (int i = 0; i < this.numPostRequests; i++){
            LiftRide liftRide = new LiftRide();
            liftRide.time(ThreadLocalRandom.current().nextInt(this.endTime - this.startTime) + this.startTime);
            liftRide.liftID(ThreadLocalRandom.current().nextInt(this.endLiftID - this.startLiftID) + this.startLiftID);
            Integer skierID = ThreadLocalRandom.current().nextInt(this.endSkierID - this.startSkierID) + this.startSkierID;
            Integer resortID = ThreadLocalRandom.current().nextInt(this.endResortID - this.startResortID) + this.startResortID;
            Integer dayID = ThreadLocalRandom.current().nextInt( this.endDay - this.startDay) + this.startDay;
            liftRideEvent = new LiftRideEvent(liftRide, resortID, this.SEASON_ID, String.valueOf(dayID), skierID);
            try {
                Main.q.put(liftRideEvent);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
