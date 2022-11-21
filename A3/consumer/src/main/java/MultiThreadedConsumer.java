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
    private final String queue;
    Gson g = new Gson();
    private final Connection connection;
    public MultiThreadedConsumer(String queue, Connection connection) {
        this.queue = queue;
        this.connection = connection;
    }

    @Override
    public void run() {
        try {
            final Channel channel = connection.createChannel();
            channel.queueDeclare(queue, false, false, false, null);
            System.out.println(channel);
            System.out.println("Thread "+ Thread.currentThread().getId() + " waiting for messages");

            final DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
                JsonObject valueData = new JsonObject();

                JsonObject requestBody = null;
                try {
                     requestBody = g.fromJson(message, JsonObject.class);
                } catch (JsonSyntaxException e){
                    e.printStackTrace();
                    return;
                }

                // extract skier id
                Integer skierID;
                String resortID;
                String dayID;
                String key;
                try{
                    // question 1-3
                    skierID = Integer.parseInt(requestBody.get("skierID").getAsString()); // key
                    dayID = requestBody.get("dayID").getAsString();
                    valueData.add("liftID", requestBody.get("liftID"));
                    valueData.add("dayID", requestBody.get("dayID"));
                    valueData.add("seasonID", requestBody.get("seasonID"));

                    // question 4
                    resortID = requestBody.get("resortID").getAsString();
                    key = resortID +"_"+ dayID;
                    System.out.println(key);
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                    return;
                }

                // add to Jedis
                try (Jedis jedis = Consumer.pool.getResource()){
                    jedis.sadd(String.valueOf(skierID), g.toJson(valueData));
                    jedis.sadd(key, String.valueOf(skierID));
                    Consumer.pool.returnResource(jedis);
                } catch (Exception e){
                    e.printStackTrace();
                    return;
                }

                System.out.println("Thread " + Thread.currentThread().getId() + " received'");
                channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);

                // question 1: "For skier N, how many days have they skied this season?"
            };
            channel.basicConsume(queue, false, deliverCallback, consumerTag -> {});
        } catch (IOException e){
            e.printStackTrace();
        }
    }
}
