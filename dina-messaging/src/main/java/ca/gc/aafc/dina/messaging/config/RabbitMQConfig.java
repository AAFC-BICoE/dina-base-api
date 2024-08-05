package ca.gc.aafc.dina.messaging.config;

import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

import java.util.Optional;
import javax.inject.Inject;
import lombok.extern.log4j.Log4j2;

/**
 * Configuration of RabbitMQ related beans
 */
@Log4j2
@Configuration
@Conditional(MessagingConfigurationCondition.class)
public class RabbitMQConfig {

  private final RabbitMQProperties rmqProps;
  private final RabbitTemplate.ReturnsCallback returnCallback;

  @Inject
  public RabbitMQConfig(RabbitMQProperties rmqProps, Optional<RabbitTemplate.ReturnsCallback> returnCallback) {
    this.rmqProps = rmqProps;
    this.returnCallback = returnCallback.orElse(null);
  }

  @Bean
  protected ConnectionFactory createConnectionFactory() {
    CachingConnectionFactory cachingConnectionFactory = new CachingConnectionFactory(rmqProps.getHost());
    cachingConnectionFactory.setUsername(rmqProps.getUsername());
    cachingConnectionFactory.setPassword(rmqProps.getPassword());

    // allow to get messages that can't be delivered back
    cachingConnectionFactory.setPublisherReturns(true);

    if (rmqProps.getPort() > 0) {
      cachingConnectionFactory.setPort(rmqProps.getPort());
    }

    return cachingConnectionFactory;
  }

  @Bean
  protected MessageConverter createMessageConverter() {
    return new Jackson2JsonMessageConverter();
  }

  @Bean
  public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
    final RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
    rabbitTemplate.setMessageConverter(createMessageConverter());

    // tell RabbitMQ that messages need to be delivered
    rabbitTemplate.setMandatory(true);

    if (returnCallback != null) {
      rabbitTemplate.setReturnsCallback(returnCallback);
    } else {
      rabbitTemplate.setReturnsCallback(
        returned -> log.error("Can't deliver message {}", returned.getMessage()));
    }

    return rabbitTemplate;
  }

}
