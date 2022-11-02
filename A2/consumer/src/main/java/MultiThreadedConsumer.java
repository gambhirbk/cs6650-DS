import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.DeliverCallback;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class MultiThreadedConsumer implements Runnable {
    private final String queue;
    Gson g = new Gson();
    private final Connection connection;

    private static ConcurrentHashMap<Integer, List<String>> liftRideMap = new ConcurrentHashMap<>();

    public MultiThreadedConsumer(String queue, Connection connection) {
        this.queue = queue;
        this.connection = connection;
    }

    @Override
    public void run() {

        try {
            final Channel channel = connection.createChannel();
            channel.queueDeclare(queue, false, false, false, null);

            final DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
                channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);

                JsonObject requestBody = g.fromJson(message, JsonObject.class);

                // extract skier id
                Integer skierID = Integer.parseInt(requestBody.get("skierID").getAsString());
                liftRideMap.computeIfAbsent(skierID, k -> new ArrayList<>());

                List<String> liftRides = liftRideMap.get(skierID);
                liftRides.add(message);

                liftRideMap.put(skierID, liftRides);
            };
            channel.basicConsume(queue, false, deliverCallback, consumerTag -> {});
        } catch (IOException e){
            e.printStackTrace();
        }
    }
}
