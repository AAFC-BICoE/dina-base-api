package ca.gc.aafc.dina.messaging;

public interface DinaEventPublisher<T> {
  void addEvent(T event);
}
