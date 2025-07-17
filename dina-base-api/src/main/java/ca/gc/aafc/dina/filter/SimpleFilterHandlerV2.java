package ca.gc.aafc.dina.filter;

import ca.gc.aafc.dina.exception.UnknownAttributeException;
import ca.gc.aafc.dina.jpa.JsonbKeyValuePredicate;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.querydsl.core.types.Ops;

import java.lang.reflect.AccessibleObject;
import lombok.NonNull;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.Type;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.Metamodel;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.function.BiFunction;
import lombok.extern.log4j.Log4j2;

/**
 * Simple filter handler for filtering by a value in a single attribute.
 * 
 * This handler has been redesigned to use the new Filter Components.
 * 
 * Example GET request where pcrPrimer's:
 * [name] == '101F' : http://localhost:8080/api/pcrPrimer?filter[name]=101F
 */
@Log4j2
public final class SimpleFilterHandlerV2 {

  private SimpleFilterHandlerV2() {
  }

  /**
   * Generates a predicate for a given filter component.
   *
   * @param cb        The criteria builder, cannot be null
   * @param root      The root type, cannot be null
   * @param parser    Lambda Expression to convert a given string value to a
   *                  given class representation of that value
   * @param metamodel JPA Metamodel
   * @return Generates a predicate for a given filter component.
   */
  public static Predicate createPredicate(
    @NonNull Root<?> root,
    @NonNull CriteriaBuilder cb,
    @NonNull BiFunction<String, Class<?>, Object> parser,
    @NonNull Metamodel metamodel,
    FilterComponent fc) {
    return createPredicate(new PredicateContext(cb, parser, metamodel), root, fc);
  }

  /**
   * Main function to create {@link Predicate} from {@link FilterComponent}.
   * @param fc
   * @return the {@link Predicate} or null if the provided {@link FilterComponent} was null
   */
  private static Predicate createPredicate(
    @NonNull PredicateContext ctx,
    @NonNull Root<?> root,
    FilterComponent fc) {

    if (fc == null) {
      return null;
    }
    
    Predicate predicate;
    switch (fc) {
      case FilterGroup fgrp -> {
        // multiple values can be submitted with en EQUALS to create an OR.
        if (fgrp.getConjunction() == FilterGroup.Conjunction.OR) {
          predicate = handleOr(ctx, root, fgrp.getComponents());
        } else {
          predicate = handleAnd(ctx, root, fgrp.getComponents());
        }
      }
      case FilterExpression fEx ->
        predicate = buildPredicate(ctx, root, fEx);
      default -> throw new IllegalStateException("Unexpected value: " + fc);
    }
    return predicate;
  }

  /**
   * Build a predicate from a {@link FilterExpression}.
   * @param filterExpression  The Filter Expression to generate the predicate from.
   * @return the predicate
   */
  private static Predicate buildPredicate(PredicateContext ctx, Root<?> root, FilterExpression filterExpression) {
    Object filterValue = filterExpression.value();

    // Using the attribute path on the component, generate a list of all the path steps.
    // "data.attributes.name" --> ["data", "attributes", "name"]
    List<String> attributePath = Arrays.asList(
      StringUtils.split(filterExpression.attribute(), '.')
    );

    Path<?> path = root;
    boolean attributeFound = false;
    Optional<Attribute<?, ?>> attribute = Optional.empty();
    for (String pathElement : attributePath) {
      attribute = findAttribute(
        ctx.metamodel(), List.of(pathElement), path.getJavaType());

      if (attribute.isPresent()) {
        path = path.get(pathElement);
        if (isBasicAttribute(attribute.get())) {
          // basic attribute start generating predicates
//            addPredicates(cb, parser, predicates, expression, path, attribute.get(),
//              attributePath);
          attributeFound = true;
          break;
        }
      }
    }

    if (!attributeFound) {
      throw new UnknownAttributeException(filterExpression.attribute());
    }

    if (filterValue == null) {
      return generateNullComparisonPredicate(ctx.cb(),
        path, filterExpression.operator());
    } else  if (isJsonb(attribute.get())) {
      try {
        return generateJsonbPredicate(ctx,
            path.getParentPath(), attributePath, attribute.get().getName(), filterValue.toString());
      } catch (JsonProcessingException e) {
        throw new IllegalArgumentException("Invalid Json filter value", e);
      }
    } else {
      return generatePredicate(ctx, path, filterExpression.operator(),
        filterValue.toString());
    }
  }

  private static Predicate generatePredicate(PredicateContext ctx, Path<?> path, Ops operator, String value) {
    return switch (operator) {
      case NE -> ctx.cb().not(ctx.cb().equal(path, ctx.parser().apply(value, path.getJavaType())));
      case EQ -> ctx.cb().equal(path, ctx.parser().apply(value, path.getJavaType()));
      case LIKE -> ctx.cb().like((Path<String>) path, value);
      // there is no built-in support for case-insensitive like in Hibernate so, we are using
      // lower case. Could have performance impact on very large tables
      case LIKE_IC -> ctx.cb().like(ctx.cb().lower((Path<String>) path), value.toLowerCase());
      default -> {
        log.warn("Unhandled operator: {}", operator);
        yield null;
      }
    };
  }

  /**
   * Generates a predicate for null comparison based on the provided operator.
   *
   * @param cb         the CriteriaBuilder used to construct the predicate
   * @param basicPath  the basic path to be compared for null
   * @param operator   the operator indicating the type of null comparison
   * @return the generated Predicate for null comparison
   */
  private static Predicate generateNullComparisonPredicate(
      @NonNull CriteriaBuilder cb, @NonNull Path<?> basicPath, @NonNull Ops operator) {
    return switch (operator.toString()) {
      case "NE" -> cb.isNotNull(basicPath);
      case "EQ", "LIKE" -> cb.isNull(basicPath);
      default -> cb.and();
    };
  }

  /**
   * Generates a JSONB predicate for querying a path based on the provided attribute path, 
   * column name, and value.
   *
   * @param ctx            Context for building the predicate
   * @param attributePath  the list of attribute paths to traverse within the JSONB structure.
   * @param columnName     the name of the column to match within the JSONB structure.
   * @param value          the value to match against the specified column name.
   * @return a Predicate representing the generated JSONB predicate.
   * @throws JsonProcessingException if an error occurs during JSON processing.
   */
  private static <E> Predicate generateJsonbPredicate(
    PredicateContext ctx,
    Path<E> root, List<String> attributePath, String columnName, String value
  ) throws JsonProcessingException {
    Queue<String> jsonbPath = new LinkedList<>(attributePath);
    while (!jsonbPath.isEmpty()) {
      if (jsonbPath.poll().equalsIgnoreCase(columnName)) {
        return JsonbKeyValuePredicate.onKey(columnName, StringUtils.join(jsonbPath, "."))
          .buildUsing(root, ctx.cb(), value, true);
      }
    }
    return null;
  }

  /**
   * Returns the attribute registered with the given meta-model found at the given
   * attribute path.
   *
   * @param metamodel     - JPA Metamodel
   * @param attributePath - list of attribute names represented by the requested
   *                      path
   * @param rootType      - Initial Owning Java class of the attribute to search
   * @return Returns the attribute found and the given attribute path.
   */
  private static <E> Optional<Attribute<?, ?>> findAttribute(
      @NonNull Metamodel metamodel,
      @NonNull List<String> attributePath,
      @NonNull Class<? extends E> rootType) {
    if (CollectionUtils.isEmpty(attributePath)) {
      return Optional.empty();
    }

    Class<?> rootJavaType = rootType;
    Attribute<?, ?> attribute = null;
    try {
      for (String pathField : attributePath) {
        attribute = metamodel.entity(rootJavaType).getAttributes()
          .stream()
          .filter(a -> a.getName().equalsIgnoreCase(pathField))
          .findFirst().orElse(null);
        if (attribute == null || isBasicAttribute(attribute)) {
          return Optional.ofNullable(attribute);
        } else {
          rootJavaType = attribute.getJavaType();
        }
      }
    } catch (IllegalArgumentException ex) {
      return Optional.empty();
    }

    return Optional.ofNullable(attribute);
  }

  /**
   * Returns true if the given attribute is basic. A basic attribute's value can
   * map directly to the column
   * value in the database.
   *
   * @param attribute attribute to evaluate
   * @return true if the given attribute is basic.
   */
  private static boolean isBasicAttribute(@NonNull Attribute<?, ?> attribute) {
    return Attribute.PersistentAttributeType.BASIC.equals(attribute.getPersistentAttributeType());
  }

  /**
   * Check if the attribute contains a JSONB type annotation on either the field
   * or the method.
   * 
   * The getJavaMember() reflects the identifying information about a single
   * member (a field or a method) where is where the annotation is stored.
   * 
   * The getJavaMember().getName() will return the method or field name.
   */
  private static boolean isJsonb(@NonNull Attribute<?, ?> attribute) {
    Class<?> clazz = attribute.getJavaMember().getDeclaringClass();

    // Check field for annotation.
    AccessibleObject ao = safeGetDeclaredField(clazz, attribute.getName());
    if (ao != null && ao.isAnnotationPresent(Type.class) &&
        ao.getAnnotation(Type.class).type().equals("jsonb")) {
      return true;
    }

    // If no annotation is present on the field, check the method instead.
    ao = safeGetDeclaredMethod(clazz, attribute.getJavaMember().getName());
    if (ao != null && ao.isAnnotationPresent(Type.class) &&
        ao.getAnnotation(Type.class).type().equals("jsonb")) {
      return true;
    }

    // Could not find the annotation, not detected as jsonb.
    return false;
  }

  private static Predicate handleOr(PredicateContext ctx, Root<?> root, List<FilterComponent> orList) {
    List<Predicate> predicates = new ArrayList<>();
    for (FilterComponent fc : orList) {
      switch(fc) {
        case FilterGroup fg -> predicates.add(createPredicate(ctx, root, fg));
        case FilterExpression fex -> predicates.add(buildPredicate(ctx, root, fex));
        default -> throw new IllegalStateException("Unexpected value: " + fc);
      }
    }

    if (!predicates.isEmpty()) {
      return ctx.cb().or(predicates.toArray(Predicate[]::new));
    }
    return null;
  }

  private static Predicate handleAnd(PredicateContext ctx, Root<?> root, List<FilterComponent> andList) {
    List<Predicate> predicates = new ArrayList<>();
    for (FilterComponent fc : andList) {
      switch(fc) {
        case FilterGroup fg -> predicates.add(createPredicate(ctx, root, fg));
        case FilterExpression fex -> predicates.add(buildPredicate(ctx, root, fex));
        default -> throw new IllegalStateException("Unexpected value: " + fc);
      }
    }

    if (!predicates.isEmpty()) {
      return ctx.cb().and(predicates.toArray(Predicate[]::new));
    }
    return null;
  }

  private static Field safeGetDeclaredField(Class<?> clazz, String attributeName) {
    try {
      return clazz.getDeclaredField(attributeName);
    } catch (NoSuchFieldException e) {
      // ignore
    }
    return null;
  }

  private static Method safeGetDeclaredMethod(Class<?> clazz, String methodName) {
    try {
      return clazz.getDeclaredMethod(methodName);
    } catch (NoSuchMethodException e) {
      // ignore
    }
    return null;
  }

  public record PredicateContext(CriteriaBuilder cb, BiFunction<String, Class<?>, Object> parser,
                                 Metamodel metamodel) {
  }

}
