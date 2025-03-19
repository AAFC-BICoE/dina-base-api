package ca.gc.aafc.dina.messaging;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import lombok.extern.log4j.Log4j2;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.TaskScheduler;

/**
 * Accumulates unique events and publish them at a regular interval.
 * Requires the config to have @EnableScheduling so a taskScheduler can be provided
 * @param <T>
 */
@Log4j2
//@Component
public class EventAccumulator<T> {

  private final TaskScheduler taskScheduler;
  private final ApplicationEventPublisher eventPublisher;

  private final Set<T> events = new HashSet<>();

  private boolean isTaskSchedulerRunning = false;
  private ScheduledFuture<?> scheduledFuture;

  public EventAccumulator(TaskScheduler taskScheduler, ApplicationEventPublisher eventPublisher) {
    this.taskScheduler = taskScheduler;
    this.eventPublisher = eventPublisher;
  }

  public synchronized void addEvent(T event) {
    events.add(event);
    if (!isTaskSchedulerRunning) {
      startTaskScheduler();
    }
  }

  private synchronized void startTaskScheduler() {
    isTaskSchedulerRunning = true;
    scheduledFuture = taskScheduler.schedule(this::trigger, Instant.now().plusSeconds(2));
  }

  private synchronized void trigger() {
    if (!events.isEmpty()) {
      for(T event : events) {
        eventPublisher.publishEvent(event);
      }

      // Clear the list after sending
      events.clear();
    }
    // Stop the timer
    isTaskSchedulerRunning = false;
    // Cancel the scheduled task to prevent memory leaks
    if (scheduledFuture != null) {
      scheduledFuture.cancel(false);
    }
  }
}
