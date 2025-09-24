package ca.gc.aafc.dina.entity;

import java.util.Optional;

/**
 * Used to represent undefined value (not provided) as well as null (explicitly set) value.
 */
public sealed interface ValueHolder<T> {

  record Defined<T>(T value) implements ValueHolder<T> {

    /**
     * Casts a {@literal Defined<?>} to {@literal Defined<T>} without changing the instance.
     * Unchecked due to type erasure; safe only when the caller knows the value of T.
     */
    @SuppressWarnings("unchecked")
    static <T> Defined<T> cast(Defined<?> d) {
      return (Defined<T>) d;
    }
  }

  record Null<T>() implements ValueHolder<T> {
  }

  record Undefined<T>() implements ValueHolder<T> {
  }

  /**
   * Returns this ValueHolder as a Defined, if it is one.
   * @return an Optional containing this as {@literal Defined<T>} if this is a Defined; otherwise Optional.empty()
   */
  default Optional<Defined<T>> asDefined() {
    return (this instanceof Defined<?> d) ? Optional.of(Defined.cast(d)) : Optional.empty();
  }

  /**
   * Returns a ValueHolder for the given value.
   * If value is null, returns a Null; otherwise returns a Defined containing the value.
   * Never returns Undefined; use {@link #undefined()} for that.
   * @param value the value to wrap, may be null
   * @return a Null if value is null; otherwise a Defined containing the value
   */
  static <T> ValueHolder<T> of(T value) {
    if (value == null) {
      return ofNull();
    }
    return new Defined<>(value);
  }

  static <T> ValueHolder<T> ofNull() {
    return new Null<>();
  }

  static <T> ValueHolder<T> undefined() {
    return new Undefined<>();
  }
}
