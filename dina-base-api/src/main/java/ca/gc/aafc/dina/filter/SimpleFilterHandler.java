package ca.gc.aafc.dina.filter;

import ca.gc.aafc.dina.jpa.JsonbValueSpecification;
import com.github.tennaito.rsql.misc.ArgumentParser;
import io.crnk.core.queryspec.FilterOperator;
import io.crnk.core.queryspec.FilterSpec;
import io.crnk.core.queryspec.QuerySpec;
import lombok.NonNull;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.Type;

import javax.annotation.Nullable;
import javax.persistence.TupleElement;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.Metamodel;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
   * @param querySpec      - crnk query spec with filters, cannot be null
   * @param cb             - the criteria builder, cannot be null
   * @param root           - the root type, cannot be null
   * @param argumentParser - used to parse the arguments into there given types. See {@link
   *                       DinaFilterArgumentParser}
   * @param metamodel      - JPA Metamodel
   * @return Generates a predicate for a given crnk filter.
   */
  public static <E> Predicate getRestriction(
    @NonNull QuerySpec querySpec,
    @NonNull Root<E> root,
    @NonNull CriteriaBuilder cb,
    @NonNull ArgumentParser argumentParser,
    @NonNull Metamodel metamodel
  ) {
    List<FilterSpec> filterSpecs = querySpec.getFilters();
    List<Predicate> predicates = new ArrayList<>();

    for (FilterSpec filterSpec : filterSpecs) {
      try {
        List<String> attributePath = filterSpec.getAttributePath();

        if (CollectionUtils.isEmpty(attributePath)) {
          break;
        }

        Path<?> basicPath = root;
        for (String pathElement : attributePath) {
          Optional<Attribute<?, ?>> attribute = SimpleFilterHandler.findBasicAttribute(
            basicPath, metamodel, List.of(pathElement));
          if (attribute.isEmpty()) {
            break;
          }
          basicPath = basicPath.get(pathElement);
          if (SimpleFilterHandler.isBasicAttribute(attribute.get())) {
            Object filterValue = filterSpec.getValue();
            if (filterValue == null) {
              predicates.add(generateNullComparisonPredicate(cb, basicPath, filterSpec.getOperator()));
            } else {
              Member javaMember = attribute.get().getJavaMember();
              String memberName = javaMember.getName();
              if (isJsonb(javaMember.getDeclaringClass().getDeclaredField(memberName))) {
                predicates.add(
                  generateJsonbPredicate(root, cb, attributePath, memberName, filterValue.toString()));
              } else {
                Object value = argumentParser.parse(filterValue.toString(), basicPath.getJavaType());
                predicates.add(cb.equal(basicPath, value));
              }
            }
          }
        }
      } catch (IllegalArgumentException | NoSuchFieldException e) {
        // This FilterHandler will ignore filter parameters that do not map to fields on the DTO,
        // like "rsql" or others that are only handled by other FilterHandlers.
      }
    }
    return cb.and(predicates.toArray(Predicate[]::new));
  }

  private static Predicate generateNullComparisonPredicate(
    CriteriaBuilder cb, @NonNull Path<?> basicPath, @NonNull FilterOperator operator
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
    Root<E> root, CriteriaBuilder cb, List<String> attributePath, String columnName, String value
  ) {
    List<String> jsonbPath = new ArrayList<>(attributePath);
    jsonbPath.removeIf(s -> s.equalsIgnoreCase(columnName)); // todo wrong use sublist
    return new JsonbValueSpecification<E>(columnName, StringUtils.join(jsonbPath, "."))
      .toPredicate(root, cb, value);
  }

  public static <E> Optional<Attribute<?, ?>> findBasicAttribute(
    @NonNull TupleElement<E> root, @NonNull Metamodel metamodel, @NonNull List<String> attributePath
  ) {
    if (CollectionUtils.isEmpty(attributePath)) {
      return Optional.empty();
    }

    Class<?> rootJavaType = root.getJavaType();
    Attribute<?, ?> attribute = null;
    for (String pathField : attributePath) {
      attribute = metamodel.managedType(rootJavaType).getAttributes()
        .stream()
        .filter(a -> a.getJavaMember().getName().equalsIgnoreCase(pathField))
        .findFirst().orElse(null);
      if (attribute == null || isBasicAttribute(attribute)) {
        break;
      } else {
        rootJavaType = attribute.getJavaType();
      }
    }
    return Optional.ofNullable(attribute);
  }

  public static boolean isBasicAttribute(@NonNull Attribute<?, ?> attribute) {
    return Attribute.PersistentAttributeType.BASIC.equals(attribute.getPersistentAttributeType());
  }

  private static boolean isJsonb(@Nullable Field declaredField) {
    if (declaredField == null) {
      return false;
    }
    Type fieldAnnotation = declaredField.getAnnotation(Type.class);
    return fieldAnnotation != null
      && StringUtils.isNotBlank(fieldAnnotation.type())
      && fieldAnnotation.type().equalsIgnoreCase("jsonb");
  }

}
