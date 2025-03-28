package ca.gc.aafc.dina.messaging;

import lombok.extern.log4j.Log4j2;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Log4j2
@Component
@ConditionalOnMissingBean(name = "event-accumulator-task-scheduler")
public class EventDirectPublisher<T> implements DinaEventPublisher<T> {

  private final ApplicationEventPublisher eventPublisher;

  public EventDirectPublisher(ApplicationEventPublisher eventPublisher) {
    this.eventPublisher = eventPublisher;
    log.info("Using EvenDirectPublisher");
  }

  @Override
  public void addEvent(T event) {
    eventPublisher.publishEvent(event);
  }
}
