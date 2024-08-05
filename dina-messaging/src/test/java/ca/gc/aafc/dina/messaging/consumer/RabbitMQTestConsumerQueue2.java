package ca.gc.aafc.dina.messaging.consumer;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import ca.gc.aafc.dina.messaging.DinaTestMessage;

import java.util.concurrent.CountDownLatch;
import lombok.Getter;

@Component
@ConditionalOnProperty(prefix = "dina.messaging", name = "isConsumer", havingValue = "true")
public class RabbitMQTestConsumerQueue2 implements RabbitMQMessageConsumer<DinaTestMessage> {

  @Getter
  private final CountDownLatch latch = new CountDownLatch(1);

  @Getter
  private DinaTestMessage messageReceived;

  @RabbitListener(queues = "#{testSecondQueueProperties.getQueue()}")
  @Override
  public void receiveMessage(DinaTestMessage message) {
    messageReceived = message;
    latch.countDown();
  }

}
