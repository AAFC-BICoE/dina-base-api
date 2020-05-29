package ca.gc.aafc.dina.mapper;

import java.util.function.Function;

import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;

/**
 * @param <E> Entity type
 */
@Builder
@Getter
public class CustomFieldResolverSpec<E> implements CustomFieldResolver<E> {
  @NonNull
  private String field;
  @NonNull
  private Function<E, Object> resolver;
}
