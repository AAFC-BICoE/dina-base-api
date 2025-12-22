package ca.gc.aafc.dina.filter;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.querydsl.core.types.Ops;

import ca.gc.aafc.dina.exception.UnknownAttributeException;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
    return switch (operator) {
      case NE -> o -> !checkValue(o, path, Objects::isNull);
      case EQ, LIKE -> o -> checkValue(o, path, Objects::isNull);
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

      // Null-last logic
      if (property1 == null && property2 == null) {
        return 0; // Both null, considered equal
      }
      if (property1 == null) {
        return 1; // property1 is null, property2 is not null, so property1 comes AFTER
      }
      if (property2 == null) {
        return -1; // property2 is null, property1 is not null, so property1 comes BEFORE
      }

      // Both are not null, compare normally
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

  /**
   * Retrieves a property's value from an object using a dot-separated path,
   * with special handling for collections.
   * If an intermediate part of the path resolves to a {@link Collection},
   * the remaining path is applied to each element within that collection.
   *
   * @param obj The object from which to retrieve the property.
   * @param propertyPath The dot-separated path to the property (e.g., "address.street", "items.price").
   * @return The resolved property value. This can be a single {@link Object} if no
   *         collections were traversed, or a {@link List} of objects if one or more
   *         collections were traversed along the path, collecting all matched values.
   *         Returns {@code null} if the object is null, the path is null/empty,
   *         or if an intermediate property is null and traversal cannot continue.
   * @throws UnknownAttributeException if an attribute in the path does not exist (e.g., no such getter method).
   * @throws RuntimeException if there are issues with property access (e.g., illegal access, invocation target exception).
   */
  private static Object getPropertyByPath(Object obj, String propertyPath)
      throws UnknownAttributeException {
    if (obj == null || propertyPath == null || propertyPath.isEmpty()) {
      return null;
    }

    String[] parts = propertyPath.split("\\.", 2);
    try {
      Object currentProperty = PropertyUtils.getNestedProperty(obj, parts[0]);

      if (parts.length == 1) { // if there is only 1 property
        return currentProperty;
      } else {
        String remainingPath = parts[1];
        if (currentProperty instanceof Collection<?> collection) {
          List<Object> values = new ArrayList<>();
          for (Object item : collection) {
            Object nestedValue = getPropertyByPath(item, remainingPath);
            // Handle collections of collections or multiple values
            if (nestedValue instanceof Collection<?> nestedCollection) {
              values.addAll(nestedCollection);
            } else if (nestedValue != null) {
              values.add(nestedValue);
            }
          }
          return values;
        } else if (currentProperty != null) {
          return getPropertyByPath(currentProperty, remainingPath);
        } else {
          return null;
        }
      }
    } catch (NoSuchMethodException e) {
      throw new UnknownAttributeException(e);
    } catch (IllegalAccessException | InvocationTargetException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Evaluates a condition against a property's value, handling potential collections.
   * If the resolved property value at the given path is a {@link Collection},
   * the condition is applied to each element in the collection, and this method
   * returns {@code true} if {@code ANY} element satisfies the condition.
   * If the resolved property value is a single object (not a Collection),
   * the condition is applied directly to that object.
   *
   * @param obj The object from which to retrieve the property.
   * @param path The dot-separated path to the property (e.g., "address.street", "items.price").
   * @param condition A function to test a single resolved property item/value.
   * @param <T> The type of the object.
   * @return {@code true} if the condition is met for any resolved value (or the single value),
   *         {@code false} otherwise.
   */
  private static <T> boolean checkValue(T obj, String path, Function<Object, Boolean> condition) {
    Object propertyValue = getPropertyByPath(obj, path);

    if (propertyValue instanceof Collection<?> collection) {
      // If it's a collection, check if ANY item in the collection satisfies the condition
      for (Object item : collection) {
        if (condition.apply(item)) {
          return true;
        }
      }
      return false;
    } else {
      // If it's a single value, just apply the condition directly
      return condition.apply(propertyValue);
    }
  }

  /**
   * Extract the property defined by path as a Comparable.
   * If the property is not directly Comparable, its String representation is used for comparison.
   * If the path resolves to a Collection, an UnsupportedOperationException is thrown as sorting
   * on collections is not directly supported by this comparator logic.
   *
   * @param o the object from which we want to extract the property
   * @param path the path of the property
   * @return a {@code Comparable<Object>} representation of the property, or null if the property itself is null.
   * @throws UnsupportedOperationException if the path resolves to a Collection.
   */
  private static Comparable<Object> propertyAsComparable(Object o, String path) {
    Object obj = getPropertyByPath(o, path);

    if (obj == null) {
      return null;
    }

    if (obj instanceof Collection) {
      throw new UnsupportedOperationException(
        "Sorting on paths that resolve to collections is not supported: " + path);
    }

    if (obj instanceof Comparable<?> comp) {
      return other -> {
        try {
          return ((Comparable<Object>) comp).compareTo(other);
        } catch (ClassCastException e) {
          log.warn("Attempted to compare incompatible types for path '{}'. Falling back to string comparison. " +
            "Object 1 type: {}, Object 2 type: {}", path, comp.getClass().getName(), other.getClass().getName());
          return comp.toString().compareTo(Objects.toString(other, "null"));
        }
      };
    }
    final String stringRepresentation = obj.toString();
    return other -> stringRepresentation.compareTo(Objects.toString(other, ""));
  }

  private static <T> Predicate<T> generatePredicate(String path, Ops operator, String value) {
    return switch (operator) {
      case NE -> Predicate.not(createEqualPredicate(path, value));
      case EQ -> createEqualPredicate(path, value);
      case IN -> createInPredicate(path, value);
      case LIKE -> createLikePredicate(path, value, true);
      case LIKE_IC -> createLikePredicate(path, value, false);
      default -> o -> false;
    };
  }

  private static <T> Predicate<T> createEqualPredicate(String path, String value) {
    return o -> checkValue(o, path, item -> Objects.equals(value, Objects.toString(item, null)));
  }

  /**
   *
   * @param path
   * @param values comma-separated
   * @return
   * @param <T>
   */
  private static <T> Predicate<T> createInPredicate(String path, String values) {
    if (values == null || values.trim().isEmpty()) {
      return o -> false;
    }
    Set<String> valueSet = QueryStringParser.parseQuotedValues(values);
    return o -> checkValue(o, path, item -> {
      if (item == null) {
        return valueSet.contains("null") || valueSet.contains("");
      }
      return valueSet.contains(item.toString());
    });
  }

  /**
   * Creates a Predicate that acts like an SQL LIKE operator (case-sensitive or not).
   *
   * @param path          the path of the attribute to check
   * @param value         the value to match. Supports '%' as a wildcard for zero or more characters
   *                      and '_' as a wildcard for exactly one character. Backslash (\) can be used to escape special characters.
   * @param caseSensitive case-sensitive or not
   * @return
   */
  private static <T> Predicate<T> createLikePredicate(String path, String value, boolean caseSensitive) {

    // Null/empty value handling
    if (value == null || value.isEmpty()) {
      return o -> false;
    }

    // ai-generated explanation of why we are using E and Q here
    // Q and E create literal sections.  This means that everything
    // between \Q and \E is interpreted literally. The .* and .
    // must be outside the \Q and \E to be special.  So we can
    // replace things like "foo%bar" with "foo\E.*\Qbar" so that the
    // .* is treated as a regex wildcard, not literally.
    String regexPattern = Pattern.quote(value)
      .replace("%", "\\E.*\\Q")
      .replace("_", "\\E.\\Q");

    final Pattern regex = caseSensitive ? Pattern.compile(regexPattern) :
      Pattern.compile(regexPattern, Pattern.CASE_INSENSITIVE);

    return o -> checkValue(o, path, item -> {
      if (item == null) {
        return false;
      }
      Matcher matcher = regex.matcher(Objects.toString(item, ""));
      return matcher.matches();
    });
  }

  private static <T> Predicate<T> handleOr(List<FilterComponent> orList) {
    if (CollectionUtils.isEmpty(orList)) {
      return o -> false; // No OR conditions = always false
    }

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
    if (CollectionUtils.isEmpty(andList)) {
      return o -> true; // No AND conditions = always true
    }

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
