import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
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
            System.out.println("Thread "+ Thread.currentThread().getId() + " waiting for messages");

            final DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                String message = new String(delivery.getBody(), StandardCharsets.UTF_8);

                JsonObject requestBody = null;
                try {
                     requestBody = g.fromJson(message, JsonObject.class);
                } catch (JsonSyntaxException e){
                    e.printStackTrace();
                    return;
                }

                // extract skier id
                Integer skierID = null;
                try{
                    skierID = Integer.parseInt(requestBody.get("skierID").getAsString());
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                    return;
                }

                liftRideMap.computeIfAbsent(skierID, k -> new ArrayList<>());

                List<String> liftRides = liftRideMap.get(skierID);
                liftRides.add(message);

                liftRideMap.put(skierID, liftRides);

                System.out.println("Thread " + Thread.currentThread().getId() + " received'");
                channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
            };
            channel.basicConsume(queue, false, deliverCallback, consumerTag -> {});
        } catch (IOException e){
            e.printStackTrace();
        }
    }
}
