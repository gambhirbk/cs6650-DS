package servlet;

import com.google.gson.Gson;
import model.Message;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.*;
import java.io.BufferedReader;
import java.io.IOException;

@WebServlet(name = "SkierServlet", urlPatterns = "/skiers/*")
public class SkierServlet extends HttpServlet {

    private Gson gson = new Gson();
    private String msg;

    private final String SEASONS_PARAMETER = "seasons";
    private final String DAYS_PARAMETER = "days";
    private final String SKIERS_PARAMETER = "skiers";
    private final int DAY_ID_MIN = 1;
    private final int DAY_ID_MAX = 365;

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
        }

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

        if (!isUrlValid(urlParts, request)) {
            Message message = new Message("string");
            response.getWriter().write(gson.toJson(message));
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            // SC_NOT_FOUND
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
                        && Integer.valueOf(urlPath[5]) >= DAY_ID_MIN
                        && Integer.valueOf(urlPath[5]) < DAY_ID_MAX
                        && urlPath[2].equals(SEASONS_PARAMETER)
                        && urlPath[4].equals(DAYS_PARAMETER)
                        && urlPath[6].equals(SKIERS_PARAMETER));
            } catch (Exception e) {
                return false;
            }
        }
        return false;
    }
}
