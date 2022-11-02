import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;

public class Consumer {

    protected final static String QUEUE_NAME = "liftRide";
    private final static Integer MAX_THREADS = 32;

    public static void main(String[] args) throws IOException, TimeoutException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("35.87.116.195");
        factory.setUsername("test");
        factory.setPassword("test");

        Connection connection = factory.newConnection();

        ExecutorService pool = Executors.newFixedThreadPool(MAX_THREADS);

        for (int i=0; i<MAX_THREADS; i++){
            pool.execute(new MultiThreadedConsumer(QUEUE_NAME, connection));
        }
    }
}
