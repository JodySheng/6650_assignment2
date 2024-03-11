package Consumer;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;


import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;


public class MsgConsumer {

    static ConnectionFactory factory = new ConnectionFactory();
    static String rabbitmqUrl = "172.31.16.86";
    static int connectionSize = 20;
    static String queueName = "skierQueue";

    public static ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(connectionSize);

    public static void main(String[] args) {
        factory.setHost(rabbitmqUrl);

        for (int i = 0; i < connectionSize; i++) {
            executor.execute(new ConsumerThread());
        }
    }


}

class ConsumerThread implements Runnable {

    @Override
    public void run() {
        try {
            // 创建连接
            Connection connection = MsgConsumer.factory.newConnection();
            // 创建通道
            Channel channel = connection.createChannel();
            // 声明要消费的队列
            channel.queueDeclare(MsgConsumer.queueName, false, false, false, null);
            // 创建队列消费者
            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                String message = new String(delivery.getBody(), "UTF-8");
                System.out.println(" [x] Received '" + message + "'");
            };

            channel.basicConsume(MsgConsumer.queueName, true, deliverCallback, consumerTag -> { });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
