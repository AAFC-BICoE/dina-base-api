package ca.gc.aafc.dina.filter;

import ca.gc.aafc.dina.jpa.JsonbValueSpecification;
import ca.gc.aafc.dina.repository.SelectionHandler;
import com.github.tennaito.rsql.misc.ArgumentParser;
import io.crnk.core.queryspec.FilterOperator;
import io.crnk.core.queryspec.FilterSpec;
import io.crnk.core.queryspec.QuerySpec;
import lombok.NonNull;
import lombok.SneakyThrows;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.Type;

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
      Path<?> attributePath;
      try {
        attributePath = SelectionHandler.handleJoins(root, filterSpec.getAttributePath(), metamodel);
        predicates.add(generatePredicate(filterSpec, attributePath, cb, argumentParser, root, metamodel));
      } catch (IllegalArgumentException e) {
        // This FilterHandler will ignore filter parameters that do not map to fields on the DTO,
        // like "rsql" or others that are only handled by other FilterHandlers.
        e.printStackTrace();
      }
    }

    return cb.and(predicates.toArray(Predicate[]::new));
  }

  /**
   * Generates a predicate for a given crnk filter spec for a given attribute path. Predicate is built with
   * the given criteria builder.
   *
   * @param filter         - filter to parse
   * @param path           - path to the attribute
   * @param cb             - criteria builder to build the predicate
   * @param argumentParser - the argument parser
   * @param metamodel      - JPA Metamodel
   * @return a predicate for a given crnk filter spec
   */
  @SneakyThrows
  private static <E> Predicate generatePredicate(
    @NonNull FilterSpec filter,
    @NonNull Path<?> path,
    @NonNull CriteriaBuilder cb,
    @NonNull ArgumentParser argumentParser,
    @NonNull Root<E> root,
    Metamodel metamodel
  ) {
    Object filterValue = filter.getValue();
    List<String> attributePath = filter.getAttributePath();

    if (filterValue == null) {
      return filter.getOperator() == FilterOperator.NEQ ? cb.isNotNull(path) : cb.isNull(path);
    } else {
      Optional<Attribute<?, ?>> attribute = findBasicAttribute(root, metamodel, attributePath);
      if (attribute.isPresent()) {
        Member javaMember = attribute.get().getJavaMember();
        String memberName = javaMember.getName();
        if (isJsonb(javaMember.getDeclaringClass().getDeclaredField(memberName))) {
          List<String> jsonbPath = new ArrayList<>(attributePath);
          jsonbPath.removeIf(s -> s.equalsIgnoreCase(memberName)); // todo wrong use sublist
          return new JsonbValueSpecification<E>(memberName, StringUtils.join(jsonbPath, "."))
            .toPredicate(root, cb, filterValue.toString());
        }
      }
      Object value = argumentParser.parse(filterValue.toString(), path.getJavaType());
      return cb.equal(path, value);
    }
  }

  public static <E> Optional<Attribute<?, ?>> findBasicAttribute(
    @NonNull TupleElement<E> root,
    @NonNull Metamodel metamodel,
    @NonNull List<String> attributePath
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

  public static boolean isBasicAttribute(Attribute<?, ?> attribute) {
    return Attribute.PersistentAttributeType.BASIC.equals(attribute.getPersistentAttributeType());
  }

  private static boolean isJsonb(Field declaredField) {
    if (declaredField == null) {
      return false;
    }
    Type fieldAnnotation = declaredField.getAnnotation(Type.class);
    return fieldAnnotation != null
      && StringUtils.isNotBlank(fieldAnnotation.type())
      && fieldAnnotation.type().equalsIgnoreCase("jsonb");
  }

}
