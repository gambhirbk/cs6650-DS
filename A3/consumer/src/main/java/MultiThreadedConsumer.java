import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.DeliverCallback;
import redis.clients.jedis.Jedis;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class MultiThreadedConsumer implements Runnable {
    private final String exchangeName;
    Gson g = new Gson();
    private final Connection connection;

    private static final String QUEUE_NAME = "rides";
    public MultiThreadedConsumer(String exchangeName, Connection connection) {
        this.exchangeName = exchangeName;
        this.connection = connection;
    }
    @Override
    public void run() {
            try {
                final Channel channel = connection.createChannel();
                channel.exchangeDeclare(exchangeName, "fanout");
                channel.queueDeclare(QUEUE_NAME, false, false, false, null);
                channel.queueBind(QUEUE_NAME, exchangeName,"");

                final DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                    String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
                    JsonObject valueData = new JsonObject();

                    JsonObject requestBody = null;
                    try {
                        requestBody = g.fromJson(message, JsonObject.class);
                    } catch (JsonSyntaxException e) {
                        e.printStackTrace();
                        return;
                    }

                    // extract skier id
                    Integer skierID;
                    try {
                        // question 1-3
                        skierID = Integer.parseInt(requestBody.get("skierID").getAsString()); // key
                        valueData.add("liftID", requestBody.get("liftID"));
                        valueData.add("dayID", requestBody.get("dayID"));
                        valueData.add("seasonID", requestBody.get("seasonID"));

                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                        return;
                    }

                    // add to Jedis
                    try (Jedis jedis = Consumer.pool.getResource()) {
                        jedis.sadd(String.valueOf(skierID), g.toJson(valueData));
                        Consumer.pool.returnResource(jedis);
                    } catch (Exception e) {
                        e.printStackTrace();
                        return;
                    }

                    channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
                };
                channel.basicConsume(QUEUE_NAME, false, deliverCallback, consumerTag -> {
                });
            } catch (IOException e) {
                e.printStackTrace();
            }
    }
}
