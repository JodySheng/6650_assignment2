package Servlet;

import com.google.gson.Gson;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;

import javax.servlet.*;
import javax.servlet.http.*;
import java.io.IOException;
import java.util.Objects;

public class SkierServlet extends HttpServlet {


    private final static String queueName = "skierQueue";

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        res.setContentType("application/json");
        ResponseMsg rmsg = new ResponseMsg();
        Gson gson = new Gson();
        String urlPath = req.getPathInfo();

        // check we have a URL!
        if (urlPath == null || urlPath.isEmpty()) {
            res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            rmsg.setMessage("Invalid inputs");
            res.getWriter().write(gson.toJson(rmsg));
            return;
        }

        String[] urlParts = urlPath.split("/");

        if (!isUrlValid(urlParts)) {
            res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            rmsg.setMessage("Invalid inputs");
            res.getWriter().write(gson.toJson(rmsg));
        } else {
            res.setStatus(HttpServletResponse.SC_CREATED);
            Skier skier = getSkier(urlParts);
            // send to rabbitmq
            try {
                Connection connection = RabbitMQConnectionPool.getConnection();
                Channel channel = connection.createChannel();
                channel.queueDeclare(queueName, false, false, false, null);
                channel.confirmSelect();
                channel.basicPublish("", queueName, null, gson.toJson(skier).getBytes());
                System.out.println("Sent '" + skier + "'");
                if (channel.waitForConfirms()) {
                    System.out.println(" [x] Message successfully sent to RabbitMQ");
                } else {
                    System.out.println(" [!] Failed to send message to RabbitMQ");
                }
                channel.close();
                RabbitMQConnectionPool.releaseConnection(connection);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            rmsg.setMessage("Write successful");
            res.getWriter().write(gson.toJson(rmsg));
        }
    }

    private Skier getSkier(String[] urlPath) {
        Skier skier = new Skier();
        skier.setSeason(Integer.parseInt(urlPath[1]));
        skier.setYear(Integer.parseInt(urlPath[3]));
        skier.setDay(Integer.parseInt(urlPath[5]));
        skier.setSkierId(Integer.parseInt(urlPath[7]));
        return skier;
    }

    private boolean isUrlValid(String[] urlPath) {
        // TODO: validate the request url path according to the API spec
        // urlPath  = "/1/seasons/2019/days/1/skiers/123"
        // urlParts = [, 1, seasons, 2019, days, 1, skiers, 123]

        if (urlPath.length != 8) {
            return false;
        }
        try {
            int season = Integer.parseInt(urlPath[1]);

            if (!Objects.equals(urlPath[2], "seasons")){
                return false;
            }
            int year = Integer.parseInt(urlPath[3]);

            if (!Objects.equals(urlPath[4], "days")){
                return false;
            }
            int day = Integer.parseInt(urlPath[5]);
            if (day <  1 || day > 366) return false;
            if (!Objects.equals(urlPath[6], "skiers")){
                return false;
            }
            int skierId = Integer.parseInt(urlPath[7]);

            return true;
        } catch (NumberFormatException e) {
            return false;
        }

    }
}
