package ca.gc.aafc.seqdb.testsupport.factories;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.lang3.RandomStringUtils;

/**
 * Base class for all the entity factories to extend from. Factories are located inside the
 * ca.gc.aafc.factory package where EntityFactoryTest will scan for them and test for validation.
 * 
 */
public interface TestableEntityFactory<T> {
 
  /**
   * Any Class extending TestableEntityFactory is required to return a entity instance built from
   * their factory. This entity should fulfill the validation/persistence constrains on the entity's
   * field annotations (Most common are @NotNull, @Size and @UniqueConstraint).
   * @return A java bean entity
   */
  T getEntityInstance();
  
  /**
   * Factories that implement this can call on this method to return a list of entities.
   * 
   * @param qty           - The number of entities in the list
   * @param supplier      - A function that returns the FactoryBuilder (i.e.
   *                      FactoryBuilder::newFactory)
   * @param configuration - A BiFunction to specify which attribute to increment, if null the method
   *                      will just call the supplier.
   * @param buildFct      - The buildFunction for the entityBuilder, reference it using a method
   *                      reference operator(EntityBuilder::build)
   * @return A list containing the number of entities specified by the qty argument.
   */
  static <S, R> List<R> newEntity(int qty, Supplier<S> supplier,
      BiFunction<S, Integer, S> configuration, Function<S, R> buildFct) {
    return IntStream.range(0, qty)
        .mapToObj(index -> configuration == null ? supplier.get()
            : configuration.apply(supplier.get(), index))
        .map(buildFct).collect(Collectors.toList());
  }
  
  /**
   * Generate a random string to use for testing any entity properties that require a string.
   * 
   * Note that the string can contain numbers and letters.
   * 
   * @param count length of the random string
   * @return A randomized string containing letters and numbers based on the count provided.
   */
  static String generateRandomName(int count) {
    return RandomStringUtils.random(count, true, true);
  }
  
  /**
   * Generate a random string to use for testing any entity properties that require a string.
   * 
   * Note that the string will contain letters only.
   * 
   * @param count length of the random string
   * @return A randomized string containing letters based on the count provided.
   */
  static String generateRandomNameLettersOnly(int count) {
    return RandomStringUtils.random(count, true, false);
  }

}
