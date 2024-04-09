package ca.gc.aafc.dina.filter;

import org.apache.commons.beanutils.PropertyUtils;

import com.querydsl.core.types.Ops;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import lombok.NonNull;
import lombok.extern.log4j.Log4j2;

/**
 * Generates {@link Predicate} based on one or more {@link FilterComponent}.
 *
 */
@Log4j2
public final class SimpleObjectFilterHandlerV2 {

  private SimpleObjectFilterHandlerV2() {
  }

  /**
   * Build a predicate from a List of {@link FilterComponent}.
   * @param predicate can be null otherwise the generated predicate will be added using AND.
   * @param filters  Must be a list of FilterExpression.
   * @return the predicate
   */
  public static <T> Predicate<T> buildPredicate(Predicate<T> predicate,
                                                List<FilterComponent> filters) {

    Predicate<T> newPredicate = predicate;
    for (FilterComponent component : filters) {
      // Only FilterExpression are supported for simple filters.
      if (component instanceof FilterExpression expression) {
        try {
          if(newPredicate == null) {
            newPredicate = buildPredicate(expression);
          } else {
            newPredicate = newPredicate.and(buildPredicate(expression));
          }
        } catch (IllegalArgumentException e) {
          throw new IllegalArgumentException("Invalid Json filter value", e);
        }
      } else {
        log.info("Ignoring FilterComponent that is not an instance of FilterExpression");
      }
    }

    return newPredicate;
  }

  /**
   * Build a predicate from a {@link FilterExpression}.
   * @param filterExpression  The Filter Expression to generate the predicate from.
   * @return the predicate
   */
  public static <T> Predicate<T> buildPredicate(FilterExpression filterExpression){
    Object filterValue = filterExpression.value();
    if (filterValue == null) {
      return generateNullComparisonPredicate(filterExpression.attribute(), filterExpression.operator());
    } else {
      return generatePredicate(filterExpression.attribute(), filterExpression.operator(), filterValue.toString());
    }
  }

  /**
   * Generates a predicate for null comparison based on the provided operator.
   *
   * @param operator the operator indicating the type of null comparison
   * @return the generated Predicate for null comparison
   */
  private static <T> Predicate<T> generateNullComparisonPredicate(String path, @NonNull Ops operator) {

    Predicate<T> isNullPredicate = o -> {
      try {
        return Objects.isNull(PropertyUtils.getNestedProperty(o, path));
      } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
        throw new RuntimeException(e);
      }
    };

    return switch (operator) {
      case NE -> Predicate.not(isNullPredicate);
      case EQ, LIKE -> isNullPredicate;
      default -> o -> false;
    };
  }

  private static <T> Predicate<T> generatePredicate(String path, Ops operator, String value) {

    Predicate<T> isEqualPredicate = o -> {
      try {
        return Objects.equals(value, PropertyUtils.getNestedProperty(o, path));
      } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
        throw new RuntimeException(e);
      }
    };

    return switch (operator) {
      case NE -> Predicate.not(isEqualPredicate);
      case EQ -> isEqualPredicate;
      default -> o -> false;
    };
  }

}
