package ca.gc.aafc.dina.filter;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.querydsl.core.types.Ops;

import java.lang.reflect.InvocationTargetException;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import lombok.NonNull;
import lombok.extern.log4j.Log4j2;

/**
 * Generates java.util {@link Predicate} based on one or more {@link FilterComponent}.
 *
 */
@Log4j2
public final class SimpleObjectFilterHandlerV2 {

  private SimpleObjectFilterHandlerV2() {
  }

  /**
   * Main function to create {@link Predicate} from {@link FilterComponent}.
   * @param fc
   * @return the {@link Predicate} or null if the provided {@link FilterComponent} was null
   */
  public static <T> Predicate<T> createPredicate(FilterComponent fc) {

    if (fc == null) {
      return null;
    }

    Predicate<T> predicate;
    switch (fc) {
      case FilterGroup fgrp -> {
        // multiple values can be submitted with en EQUALS to create an OR.
        if (fgrp.getConjunction() == FilterGroup.Conjunction.OR) {
          predicate = handleOr(fgrp.getComponents());
        } else {
          predicate = handleAnd(fgrp.getComponents());
        }
      }
      case FilterExpression fEx ->
        predicate = buildPredicate(fEx);
      default -> throw new IllegalStateException("Unexpected value: " + fc);
    }

    return predicate;
  }

  /**
   * Build a predicate from a {@link FilterExpression}.
   * @param filterExpression  The Filter Expression to generate the predicate from.
   * @return the predicate
   */
  private static <T> Predicate<T> buildPredicate(FilterExpression filterExpression) {
    Object filterValue = filterExpression.value();
    if (filterValue == null) {
      return generateNullComparisonPredicate(filterExpression.attribute(),
        filterExpression.operator());
    } else {
      return generatePredicate(filterExpression.attribute(), filterExpression.operator(),
        filterValue.toString());
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

  /**
   * Generates a null- (null last) comparator for ordering based on a path.
   * Reverse order is identified by a dash (-) prefix.
   * @param sortPath
   * @return a comparator
   */
  public static <T> Comparator<T> generateComparator(String sortPath) {
    Objects.requireNonNull(sortPath);

    final boolean isReverse = sortPath.startsWith("-");
    final String path = isReverse ? StringUtils.removeStart(sortPath, "-") : sortPath;

    Comparator<T> comparator = (o1, o2) -> {
      Comparable<Object> property1 = propertyAsComparable(o1, path);
      Object property2 = getPropertyByPath(o2, path);

      if (property1 == null) {
        return 1;
      }
      if (property2 == null) {
        return 0;
      }
      return property1.compareTo(property2);
    };

    if (isReverse) {
      return comparator.reversed();
    }
    return comparator;
  }

  /**
   * Generates a null- (null last) comparator for ordering based on a list of path.
   * Reverse order is identified by a dash (-) prefix.
   * @param sortList
   * @return the combined comparator or null if sortList is empty or null
   */
  public static <T> Comparator<T> generateComparator(List<String> sortList) {
    Comparator<T> comparator = null;
    if (CollectionUtils.isNotEmpty(sortList)) {
      for (String sort : sortList) {
        if (comparator == null) {
          comparator = generateComparator(sort);
        } else {
          comparator =
            comparator.thenComparing(generateComparator(sort));
        }
      }
    }
    return comparator;
  }

  private static Object getPropertyByPath(Object obj, String propertyPath) {
    try {
      return PropertyUtils.getNestedProperty(obj, propertyPath);
    } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Extract the property defined by path as a Comparable.
   * @param o the object from which we want to extract the property
   * @param path the path of the property
   * @return
   */
  private static Comparable<Object> propertyAsComparable(Object o, String path) {
    Object obj = getPropertyByPath(o, path);
    if (obj instanceof Comparable<?> comp) {
      return (Comparable<Object>) comp;
    }
    return null;
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

  private static <T> Predicate<T> handleOr(List<FilterComponent> orList) {
    Predicate<T> predicate = null;
    for (FilterComponent fc : orList) {
      switch(fc) {
        case FilterGroup fg -> predicate = or(predicate, createPredicate(fg));
        case FilterExpression fex -> predicate = or(predicate, buildPredicate(fex));
        default -> throw new IllegalStateException("Unexpected value: " + fc);
      }
    }
    return predicate;
  }

  private static <T> Predicate<T> handleAnd(List<FilterComponent> andList) {
    Predicate<T> predicate = null;
    for (FilterComponent fc : andList) {
      switch (fc) {
        case FilterGroup fg -> predicate = and(predicate, createPredicate(fg));
        case FilterExpression fex -> predicate = and(predicate, buildPredicate(fex));
        default -> throw new IllegalStateException("Unexpected value: " + fc);
      }
    }
    return predicate;
  }

  /**
   * null-safe AND predicate handling
   * @param current
   * @param toAdd
   * @return
   */
  private static <T> Predicate<T> and(Predicate<T> current, Predicate<T> toAdd) {
    if (current == null) {
      return toAdd;
    }
    return current.and(toAdd);
  }

  /**
   * null-safe OR predicate handling
   * @param current
   * @param toAdd
   * @return
   */
  private static <T> Predicate<T> or(Predicate<T> current, Predicate<T> toAdd) {
    if (current == null) {
      return toAdd;
    }
    return current.or(toAdd);
  }

}
