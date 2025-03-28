package ca.gc.aafc.dina.messaging;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import lombok.extern.log4j.Log4j2;

/**
 * Accumulates unique events and publish them at a regular interval.
 * Requires a task scheduler
 *
 * <pre>
 *{@code
 * @Bean(name = "event-accumulator-task-scheduler")
 * public ThreadPoolTaskScheduler taskScheduler2() {
 *   ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
 *   scheduler.setPoolSize(1);
 *   scheduler.setThreadNamePrefix("eventAccumulatorTaskScheduler-");
 *   return scheduler;
 * }
 * }
 * </pre>
 * @param <T>
 */
@Log4j2
@Component
@ConditionalOnBean(name = "event-accumulator-task-scheduler")
public class EventAccumulator<T> implements DinaEventPublisher<T> {

  private static final int DELAY_IN_SECONDS = 2;

  private final TaskScheduler taskScheduler;
  private final ApplicationEventPublisher eventPublisher;

  private final Set<T> events = new HashSet<>();

  private boolean isTaskSchedulerRunning = false;
  private ScheduledFuture<?> scheduledFuture;

  public EventAccumulator(@Qualifier("event-accumulator-task-scheduler") TaskScheduler taskScheduler, ApplicationEventPublisher eventPublisher) {
    this.taskScheduler = taskScheduler;
    this.eventPublisher = eventPublisher;
    log.info("Using EventAccumulator");
  }

  @Override
  public synchronized void addEvent(T event) {
    events.add(event);
    if (!isTaskSchedulerRunning) {
      startTaskScheduler();
    }
  }

  private synchronized void startTaskScheduler() {
    isTaskSchedulerRunning = true;
    scheduledFuture = taskScheduler.schedule(this::trigger, Instant.now().plusSeconds(DELAY_IN_SECONDS));
  }

  private synchronized void trigger() {
    if (!events.isEmpty()) {
      for (T event : events) {
        eventPublisher.publishEvent(event);
      }

      // Clear the list after sending
      events.clear();
    }
    isTaskSchedulerRunning = false;
    // Cancel the scheduled task
    if (scheduledFuture != null) {
      scheduledFuture.cancel(false);
    }
  }
}
