package ca.gc.aafc.dina.filter;

import ca.gc.aafc.dina.jpa.JsonbKeyValuePredicate;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.crnk.core.queryspec.FilterOperator;
import io.crnk.core.queryspec.FilterSpec;
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
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.function.BiFunction;

/**
 * Simple filter handler for filtering by a value in a single attribute. Example GET request where pcrPrimer's
 * [name] == '101F' : http://localhost:8080/api/pcrPrimer?filter[name]=101F
 */
public final class SimpleFilterHandler {

  private SimpleFilterHandler() {
  }

  /**
   * Generates a predicate for a given crnk filter.
   *
   * @param cb        - the criteria builder, cannot be null
   * @param root      - the root type, cannot be null
   * @param parser    - Lambda Expression to convert a given string value to a given class representation of
   *                  that value
   * @param metamodel - JPA Metamodel
   * @return Generates a predicate for a given crnk filter.
   */
  public static <E> Predicate getRestriction(
    @NonNull Root<E> root,
    @NonNull CriteriaBuilder cb,
    @NonNull BiFunction<String, Class<?>, Object> parser,
    @NonNull Metamodel metamodel,
    @NonNull List<FilterSpec> filters
  ) {
    List<Predicate> predicates = new ArrayList<>();
    for (FilterSpec filterSpec : filters) {
      try {
        List<String> attributePath = filterSpec.getAttributePath();

        if (CollectionUtils.isEmpty(attributePath)) {
          continue; // move to next filter spec
        }

        Path<?> path = root;
        for (String pathElement : attributePath) {
          Optional<Attribute<?, ?>> attribute = SimpleFilterHandler.findAttribute(
            metamodel, List.of(pathElement), path.getJavaType());

          if (attribute.isPresent()) {
            path = path.get(pathElement);
            if (SimpleFilterHandler.isBasicAttribute(attribute.get())) {
              // basic attribute start generating predicates
              addPredicates(cb, parser, predicates, filterSpec, path, attribute.get());
            }
          }
        }
      } catch (IllegalArgumentException | NoSuchFieldException | NoSuchMethodException e) {
        // This FilterHandler will ignore filter parameters that do not map to fields on the DTO,
        // like "rsql" or others that are only handled by other FilterHandlers.
      } catch (JsonProcessingException e) {
        throw new IllegalArgumentException("Invalid Json filter value", e);
      }
    }
    return cb.and(predicates.toArray(Predicate[]::new));
  }

  private static void addPredicates(
    CriteriaBuilder cb,
    BiFunction<String, Class<?>, Object> parser,
    @NonNull List<Predicate> predicates,
    @NonNull FilterSpec spec,
    @NonNull Path<?> path,
    @NonNull Attribute<?, ?> attribute
  ) throws NoSuchFieldException, NoSuchMethodException, JsonProcessingException {
    Object filterValue = spec.getValue();
    if (filterValue == null) {
      predicates.add(generateNullComparisonPredicate(cb, path, spec.getOperator()));
    } else {
      if (isJsonb(attribute)) {
        predicates.add(generateJsonbPredicate(
          path.getParentPath(), cb, spec.getAttributePath(), attribute.getName(), filterValue.toString()));
      } else {
        predicates.add(cb.equal(path, parser.apply(filterValue.toString(), path.getJavaType())));
      }
    }
  }

  private static Predicate generateNullComparisonPredicate(
    @NonNull CriteriaBuilder cb, @NonNull Path<?> basicPath, @NonNull FilterOperator operator
  ) {
    if (FilterOperator.NEQ.equals(operator)) {
      return cb.isNotNull(basicPath);
    } else if (FilterOperator.EQ.equals(operator) || FilterOperator.LIKE.equals(operator)) {
      return cb.isNull(basicPath);
    } else {
      return cb.and();
    }
  }

  private static <E> Predicate generateJsonbPredicate(
    Path<E> root, CriteriaBuilder cb, List<String> attributePath, String columnName, String value
  ) throws JsonProcessingException {
    Queue<String> jsonbPath = new LinkedList<>(attributePath);
    while (!jsonbPath.isEmpty()) {
      if (jsonbPath.poll().equalsIgnoreCase(columnName)) {
        return JsonbKeyValuePredicate.onKey(columnName, StringUtils.join(jsonbPath, "."))
          .buildUsing(root, cb, value, true);
      }
    }
    return cb.and();
  }

  /**
   * Returns the attribute registered with the given meta-model found at the given attribute path.
   *
   * @param metamodel     - JPA Metamodel
   * @param attributePath - list of attribute names represented by the requested path
   * @param rootType      - Initial Owning Java class of the attribute to search
   * @return Returns the attribute found and the given attribute path.
   */
  private static <E> Optional<Attribute<?, ?>> findAttribute(
    @NonNull Metamodel metamodel,
    @NonNull List<String> attributePath,
    @NonNull Class<? extends E> rootType
  ) {
    if (CollectionUtils.isEmpty(attributePath)) {
      return Optional.empty();
    }

    Class<?> rootJavaType = rootType;
    Attribute<?, ?> attribute = null;
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
    return Optional.ofNullable(attribute);
  }

  /**
   * Returns true if the given attribute is basic. A basic attribute's value can map directly to the column
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
  private static boolean isJsonb(@NonNull Attribute<?,?> attribute) {
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

  private static Field safeGetDeclaredField(Class<?> clazz , String attributeName) {
    try {
      return clazz.getDeclaredField(attributeName);
    } catch (NoSuchFieldException e) {
      // ignore
    }
    return null;
  }

  private static Method safeGetDeclaredMethod(Class<?> clazz , String methodName) {
    try {
      return clazz.getDeclaredMethod(methodName);
    } catch (NoSuchMethodException e) {
      // ignore
    }
    return null;
  }

}
