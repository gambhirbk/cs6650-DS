import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.Protocol;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeoutException;

    public class Consumer {
    protected final static String EXCHANGE_NAME = "liftRide";
    protected static final JedisPool pool = new JedisPool(buildPoolConfig(), Protocol.DEFAULT_HOST, 6379);
    private final static Integer MAX_THREADS = 148;
    public static void main(String[] args) throws IOException, TimeoutException, InterruptedException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("34.216.146.87");
        factory.setUsername("test");
        factory.setPassword("test");

        Connection connection = factory.newConnection();

        CountDownLatch latch = new CountDownLatch(MAX_THREADS);
        for (int i=0; i < MAX_THREADS; i++){
            Thread thread = new Thread(new MultiThreadedConsumer(EXCHANGE_NAME, connection));
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
