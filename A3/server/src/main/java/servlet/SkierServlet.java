package servlet;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import model.Message;
import model.RequestBody;
import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeoutException;

@WebServlet(name = "SkierServlet", urlPatterns = "/skiers/*")
public class SkierServlet extends HttpServlet {

    private BlockingQueue<Channel> pool;
    private Connection connection;

    private final Integer NUM_CHANNELS = 100;

    private final String QUEUE_NAME = "liftRide";

    /**
     * Initialize the RabbitMQ Channel pool during Servlet initialization
     *
     * @throws ServletException when Servlet can't be initialized
     */

    @Override
    public void init() throws ServletException {
        super.init();
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("34.217.103.121");
        factory.setUsername("test");
        factory.setPassword("test");

        try {
            connection = factory.newConnection();
        } catch (IOException | TimeoutException e) {
            System.err.println("Unable to connect with RabbitMQ");
            e.printStackTrace();
        }

        // blocking queue for shared pool of channels
        pool = new LinkedBlockingQueue<>();
        for (int i = 0; i < NUM_CHANNELS; i++){
            try {
                Channel channel = connection.createChannel();
                channel.queueDeclare(QUEUE_NAME, false, false, false, null);
                pool.add(channel);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    private Gson gson = new Gson();
    private String msg;
    private final String SEASONS_PARAMETER = "seasons";
    private final String DAYS_PARAMETER = "days";
    private final String SKIERS_PARAMETER = "skiers";
    private final int DAY_ID_MIN = 1;
    private final int DAY_ID_MAX = 365;
    private final int LIFT_ID_MIN = 1;
    private final int LIFT_ID_MAX = 40;
    private final int TIME_MIN = 1;
    private final int TIME_MAX = 360;
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        res.setContentType("text/plain");
        String urlPath = req.getPathInfo();
        System.out.println(urlPath);

        // check we have a URL!
        if (urlPath == null || urlPath.isEmpty()) {
            res.setStatus(HttpServletResponse.SC_NOT_FOUND);
            res.getWriter().write("404: Missing url");
            return;
        }

        String[] urlParts = urlPath.split("/");
        // and now validate url path and return the response status code
        // (and maybe also some value if input is valid)
        if (!isUrlValid(urlParts, req)) {
            res.setStatus(HttpServletResponse.SC_NOT_FOUND);
            res.getWriter().write(msg);
        } else {
            res.setStatus(HttpServletResponse.SC_OK);
            // do any sophisticated processing with urlParts which contains all the url params
            // TODO: process url params in `urlParts`
            res.getWriter().write("200: It works!!!!");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        String urlPath = request.getPathInfo();

        if (urlPath == null || urlPath.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            response.getWriter().write("missing parameter");
            return;
        }

        JsonObject body = gson.fromJson(request.getReader(), JsonObject.class);
        String[] urlParts = urlPath.split("/");

        // and now validate url path and return the response status code
        // (and maybe also some value if input is valid)
        BufferedReader buffIn = null;

        try {
            buffIn = request.getReader();
            StringBuilder reqBody = new StringBuilder();
            String line;
            while ((line = buffIn.readLine()) != null) {
                reqBody.append(line);
            }
            response.getWriter().write(reqBody.toString());
        } catch (Exception e) {
            Message message = new Message("string");
            response.getWriter().write(gson.toJson(message));
            return;
        } finally {
            if (buffIn != null){
                try {
                    buffIn.close();
                } catch (IOException ex){
                    ex.printStackTrace();
                }
            }
        }

        if (isUrlValid(urlParts, request)) {
            Integer skierID = Integer.parseInt(urlParts[7]);
            Integer resortID = Integer.parseInt(urlParts[1]);
            Integer dayID = Integer.parseInt(urlParts[5]);
            String seasonID = urlParts[3];
            JsonObject mesg = createMessageToSendQueue(body, skierID, resortID, dayID, seasonID);
            response.getWriter().write(gson.toJson(mesg));
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);

            // check if parameters are valid in the request
            if (!isValidRequestBody(body, response)) return;

            try {
                Channel channel = pool.take();
                channel.basicPublish("", QUEUE_NAME, null, gson.toJson(mesg).getBytes());
                System.out.println(" Sent '" + mesg + "'");

                response.setStatus(HttpServletResponse.SC_CREATED);
                response.getWriter().write("LiftRide event created");

                pool.add(channel);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } else {
            response.setStatus(HttpServletResponse.SC_OK);
        }
    }

    private boolean isUrlValid(String[] urlPath, HttpServletRequest req) {
        if (urlPath.length == 8) {
            try {
                for (int i = 1; i < urlPath.length; i += 2) {
                    Integer.parseInt(urlPath[i]);
                }
                return (urlPath[3].length() == 4
                        && Integer.parseInt(urlPath[5]) >= DAY_ID_MIN
                        && Integer.parseInt(urlPath[5]) < DAY_ID_MAX
                        && urlPath[2].equals(SEASONS_PARAMETER)
                        && urlPath[4].equals(DAYS_PARAMETER)
                        && urlPath[6].equals(SKIERS_PARAMETER));
            } catch (Exception e) {
                return false;
            }
        }
        return false;
    }

    private JsonObject createMessageToSendQueue(JsonObject body, Integer skierID, Integer resortID, Integer dayID, String seasonID){
        JsonObject mesg = new JsonObject();
        mesg.add("time", body.get("time"));
        mesg.add("liftID", body.get("liftID"));
        mesg.add("skierID", new JsonPrimitive(skierID));
        mesg.add("resortID", new JsonPrimitive(resortID));
        mesg.add("dayID", new JsonPrimitive(dayID));
        mesg.add("seasonID", new JsonPrimitive(seasonID));
        return mesg;
    }
    private boolean isValidRequestBody(JsonObject body, HttpServletResponse response) throws IOException {
        HashMap<String, RequestBody> parameters = new HashMap<>();
        parameters.put("time", new RequestBody("time", null, TIME_MIN, TIME_MAX));
        parameters.put("liftID", new RequestBody("liftID", null, LIFT_ID_MIN, LIFT_ID_MAX));

        for (String param: parameters.keySet()){
            JsonElement val = body.get(param);
            RequestBody bodyParam = parameters.get(param);
            bodyParam.setValue(val.getAsString());

            if (val == null){
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write(param + "is missing");
                return false;
            }
            if (!bodyParam.isValidValue(response)) return false;
        }
        return true;
    }
}
