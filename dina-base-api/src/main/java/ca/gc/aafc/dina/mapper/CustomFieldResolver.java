package ca.gc.aafc.dina.mapper;

import java.util.function.Function;

/**
 * Used to Resolve fields where a custom function needs to be applied.
 * 
 * @param <E>
 *              - Type of input for the resolver function
 */
public interface CustomFieldResolver<E> {

  String getField();

  Function<E, Object> getResolver();

}
