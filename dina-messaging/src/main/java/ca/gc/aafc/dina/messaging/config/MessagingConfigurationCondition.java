package ca.gc.aafc.dina.messaging.config;

import org.springframework.boot.autoconfigure.condition.AnyNestedCondition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

/**
 * Condition that returns true if messaging consumer OR producer is set.
 * This class introduce a condition based on values coming from 2 different @ConditionalOnProperty
 * values. Values1 and value2 are evaluated (true/false) and combined with an OR operator.
 *
 */
public class MessagingConfigurationCondition extends AnyNestedCondition {

  public MessagingConfigurationCondition() {
    super(ConfigurationPhase.PARSE_CONFIGURATION);
  }

  @ConditionalOnProperty(prefix = "dina.messaging", name = "isConsumer", havingValue = "true")
  static class Value1Condition {
  }

  @ConditionalOnProperty(prefix = "dina.messaging", name = "isProducer", havingValue = "true")
  static class Value2Condition {
  }
}
