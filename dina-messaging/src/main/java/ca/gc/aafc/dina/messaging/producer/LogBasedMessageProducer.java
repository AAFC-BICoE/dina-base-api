package ca.gc.aafc.dina.messaging.producer;

import lombok.extern.log4j.Log4j2;

import ca.gc.aafc.dina.messaging.DinaMessage;

/**
 * Log4j2 based {@link MessageProducer} used mostly to run in dev mode.
 * Should be used with @ConditionalOnMissingBean if required by a module.
 * Not active by default since it's up to the module to define the default behavior.
 */
@Log4j2
public class LogBasedMessageProducer implements MessageProducer {

  public void send(DinaMessage dinaMessage) {
    log.info("Message produced : {}", dinaMessage::toString);
  }
}
