package ca.gc.aafc.dina.messaging.consumer;

import java.util.concurrent.CountDownLatch;
import lombok.Getter;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import ca.gc.aafc.dina.messaging.DinaTestMessage;

@Component
public class RabbitMQTestConsumer implements RabbitMQMessageConsumer<DinaTestMessage> {

  @Getter
  private final CountDownLatch latch = new CountDownLatch(1);

  @Getter
  private DinaTestMessage messageReceived;

  @RabbitListener(queues = "#{testQueueProperties.getQueue()}")
  @Override
  public void receiveMessage(DinaTestMessage message) {
    messageReceived = message;
    latch.countDown();
  }

}
