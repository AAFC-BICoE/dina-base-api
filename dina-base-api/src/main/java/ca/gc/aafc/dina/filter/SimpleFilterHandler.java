package ca.gc.aafc.dina.filter;

import ca.gc.aafc.dina.jpa.JsonbValueSpecification;
import ca.gc.aafc.dina.repository.SelectionHandler;
import com.github.tennaito.rsql.misc.ArgumentParser;
import io.crnk.core.queryspec.FilterOperator;
import io.crnk.core.queryspec.FilterSpec;
import io.crnk.core.queryspec.QuerySpec;
import lombok.NonNull;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.Type;
import org.hibernate.query.criteria.internal.path.SingularAttributePath;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.util.ArrayList;
import java.util.List;

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
   * @return Generates a predicate for a given crnk filter.
   */
  public static <E> Predicate getRestriction(
    @NonNull QuerySpec querySpec,
    @NonNull Root<E> root,
    @NonNull CriteriaBuilder cb,
    @NonNull ArgumentParser argumentParser
  ) {
    List<FilterSpec> filterSpecs = querySpec.getFilters();
    List<Predicate> predicates = new ArrayList<>();

    for (FilterSpec filterSpec : filterSpecs) {
      Path<?> attributePath;
      try {
        attributePath = SelectionHandler.getExpression(root, filterSpec.getAttributePath());
        predicates.add(generatePredicate(filterSpec, attributePath, cb, argumentParser, root));
      } catch (IllegalArgumentException e) {
        // This FilterHandler will ignore filter parameters that do not map to fields on the DTO,
        // like "rsql" or others that are only handled by other FilterHandlers.
      }
    }

    return cb.and(predicates.toArray(Predicate[]::new));
  }

  /**
   * Generates a predicate for a given crnk filter spec for a given attribute path. Predicate is built with
   * the given criteria builder.
   *
   * @param filter         - filter to parse
   * @param path  - path to the attribute
   * @param cb             - criteria builder to build the predicate
   * @param argumentParser - the argument parser
   * @return a predicate for a given crnk filter spec
   */
  @SneakyThrows
  private static <E> Predicate generatePredicate(
    @NonNull FilterSpec filter,
    @NonNull Path<?> path,
    @NonNull CriteriaBuilder cb,
    @NonNull ArgumentParser argumentParser,
    @NonNull Root<E> root
  ) {
    Object filterValue = filter.getValue();
    if (filterValue == null) {
      return filter.getOperator() == FilterOperator.NEQ ? cb.isNotNull(path) : cb.isNull(path);
    } else {
      if (path instanceof SingularAttributePath) {
        SingularAttributePath<?> singularAttributePath = (SingularAttributePath<?>) path;
        Member javaMember = singularAttributePath.getAttribute().getJavaMember();
        String memberName = javaMember.getName();
        if (isJsonb(javaMember.getDeclaringClass().getDeclaredField(memberName))) {
          List<String> jsonbPath = new ArrayList<>(filter.getAttributePath());
          jsonbPath.removeIf(s -> s.equalsIgnoreCase(memberName));
          return new JsonbValueSpecification<E>(memberName, StringUtils.join(jsonbPath, "."))
            .toPredicate(root, cb, filterValue.toString());
        }
      }
      Object value = argumentParser.parse(filterValue.toString(), path.getJavaType());
      return cb.equal(path, value);
    }
  }

  private static boolean isJsonb(Field declaredField) {
    return declaredField != null
      && declaredField.getAnnotation(Type.class) != null
      && StringUtils.isNotBlank(declaredField.getAnnotation(Type.class).type())
      && declaredField.getAnnotation(Type.class).type().equalsIgnoreCase("jsonb");
  }

}
