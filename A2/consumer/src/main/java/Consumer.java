import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeoutException;

    public class Consumer {
    protected final static String QUEUE_NAME = "liftRide";
    private final static Integer MAX_THREADS = 32;
    public static void main(String[] args) throws IOException, TimeoutException, InterruptedException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("54.201.103.243");
        factory.setUsername("test");
        factory.setPassword("test");

        Connection connection = factory.newConnection();
//        ExecutorService pool = Executors.newFixedThreadPool(MAX_THREADS);

        CountDownLatch latch = new CountDownLatch(MAX_THREADS);
        for (int i=0; i < MAX_THREADS; i++){
            Thread thread = new Thread(new MultiThreadedConsumer(QUEUE_NAME, connection));
            thread.start();
        }
        latch.await();
    }
}
