import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeoutException;

    public class Consumer {
    protected final static String QUEUE_NAME = "liftRide";

    private final static String EC2_REDIS_HOST = "35.93.76.176";

    protected static final JedisPool pool = new JedisPool(buildPoolConfig(), EC2_REDIS_HOST, 6379);
    private final static Integer MAX_THREADS = 148;
    public static void main(String[] args) throws IOException, TimeoutException, InterruptedException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("34.217.103.121");
        factory.setUsername("test");
        factory.setPassword("test");

        Connection connection = factory.newConnection();

        CountDownLatch latch = new CountDownLatch(MAX_THREADS);
        for (int i=0; i < MAX_THREADS; i++){
            Thread thread = new Thread(new MultiThreadedConsumer(QUEUE_NAME, connection));
            thread.start();
        }
        latch.await();
    }

    private static JedisPoolConfig buildPoolConfig() {
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(148);
        poolConfig.setMaxWait(Duration.ofMillis(2000));
        poolConfig.setBlockWhenExhausted(true);
        poolConfig.setTestOnBorrow(true);
        return poolConfig;
    }
}
