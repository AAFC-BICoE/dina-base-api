package ca.gc.aafc.dina.mapper;

import java.util.function.Function;

import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;

/**
 * @param <E> Entity type
 * @deprecated will be removed with JPA repository classes
 */
@Builder
@Getter
@Deprecated(forRemoval = true, since = "0.42")
public class CustomFieldResolverSpec<E> {
  @NonNull
  private String field;
  @NonNull
  private Function<E, Object> resolver;
}
