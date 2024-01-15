package ca.gc.aafc.dina.messaging.config;

import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

import javax.inject.Inject;

/**
 * Configuration of RabbitMQ related beans
 */
@Configuration
@Conditional(MessagingConfigurationCondition.class)
public class RabbitMQConfig {

  private final RabbitMQProperties rmqProps;

  @Inject
  public RabbitMQConfig(RabbitMQProperties rmqProps) {
    this.rmqProps = rmqProps;
  }

  @Bean
  protected ConnectionFactory createConnectionFactory() {
    CachingConnectionFactory cachingConnectionFactory = new CachingConnectionFactory(rmqProps.getHost());
    cachingConnectionFactory.setUsername(rmqProps.getUsername());
    cachingConnectionFactory.setPassword(rmqProps.getPassword());

    if(rmqProps.getPort() > 0) {
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

    return rabbitTemplate;
  }

}
