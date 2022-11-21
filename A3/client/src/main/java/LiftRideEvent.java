import io.swagger.client.model.LiftRide;

public class LiftRideEvent {

    private LiftRide liftRide;
    private Integer resortID;
    private String seasonID;
    private String dayID;
    private Integer skierID;

    public LiftRideEvent(LiftRide liftRide, Integer resortID, String seasonID, String dayID, Integer skierID) {
        this.liftRide = liftRide;
        this.resortID = resortID;
        this.seasonID = seasonID;
        this.dayID = dayID;
        this.skierID = skierID;
    }

    public LiftRide getLiftRide() {
        return liftRide;
    }

    public Integer getResortID() {
        return resortID;
    }

    public String getSeasonID() {
        return seasonID;
    }

    public String getDayID() {
        return dayID;
    }

    public Integer getSkierID() {
        return skierID;
    }

    @Override
    public String toString() {
        return "LiftRideEvent{" +
                "liftRide=" + liftRide +
                ", resortID=" + resortID +
                ", seasonID='" + seasonID + '\'' +
                ", dayID='" + dayID + '\'' +
                ", skierID=" + skierID +
                '}';
    }
}
