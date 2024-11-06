package ca.gc.aafc.dina.filter;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import ca.gc.aafc.dina.entity.DinaEntity;
import ca.gc.aafc.dina.exception.UnknownAttributeException;
import ca.gc.aafc.dina.mapper.DinaMappingRegistry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.FetchParent;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Root;
import lombok.NonNull;
import lombok.extern.log4j.Log4j2;

/**
 * Rewrite of DinaFilterResolver without dependencies on Crnk
 */
@Log4j2
public final class EntityFilterHelper {

  public static final String REVERSE_ORDER_PREFIX = "-";

  private EntityFilterHelper() {
    // utility class
  }

  /**
   * Performs LEFT JOIN fetches to join the related entities specified in the sort attributes.
   *
   * @param root The Root object to which the related entities are to be joined.
   * @param sortAttributes attributes to use to sort
   * @param resourceClass Class of the exposed resource, usually the dto
   * @param registry The DinaMappingRegistry used for mapping information
   */
  public static <E extends DinaEntity> void leftJoinSortRelations(Root<E> root,
                                                                  List<String> sortAttributes,
                                                                  Class<?> resourceClass,
                                                                  DinaMappingRegistry registry) {
    if (root == null || CollectionUtils.isEmpty(sortAttributes)) {
      return;
    }

    // Remove the last element from the sort attribute path since we want to only include the nested
    // relationship. Ex. "department.name" would retrieve "department".
    List<List<String>> relationsToJoin = new ArrayList<>();

    for (String sortAttribute : sortAttributes) {
      List<String> sortAttributeParts = Arrays.asList(StringUtils.split(sortAttribute, "."));
      relationsToJoin.add(
        parseMappableRelationshipPath(registry, resourceClass, sortAttributeParts.size() <= 1 ? sortAttributeParts
          : sortAttributeParts.subList(0, sortAttributeParts.size() - 1)));
    }
    relationsToJoin.forEach(relation -> joinAttributePath(root, relation));
  }

  /**
   * Extracts relationships represented by the provided Set and ensures their validity.
   *
   * This method extracts the relationships (attribute paths) and validates
   * them using the given DinaMappingRegistry. The relationships are represented as chains of relationship
   * names separated by dots, and nested relationships will be broken up to individual components.
   * For example, "departments.employees" represents a join between the Department entity and the Employee
   * entity via the "employees" attribute.
   * @param includes set of include attribute(s)
   * @param resourceClass resource class, usually the Dto
   * @param registry  The DinaMappingRegistry used for mapping information to validate the relationships.
   * @return A Set containing the extracted relationships.
   *         The relationships are represented as valid chains of relationship names separated by dots.
   *         If no relationships are present, an empty Set is returned.
   */
  public static Set<String> extractRelationships(Set<String> includes, Class<?> resourceClass, DinaMappingRegistry registry) {

    if (CollectionUtils.isEmpty(includes)) {
      return Set.of();
    }

    Set<String> relationsToJoin = new HashSet<>();
    for (String i : includes) {
      List<String> attributePath = List.of(StringUtils.split(i, "."));
      List<String> mappablePath = parseMappableRelationshipPath(registry, resourceClass, attributePath);
      if (!mappablePath.isEmpty()) {
        relationsToJoin.add(String.join(".", mappablePath));
      }
    }
    return relationsToJoin;
  }

  /**
   * Parses a list of String representing the sort attribute(s).
   * Return a list of {@link Order} built against the provided {@link CriteriaBuilder}
   * and {@link Path}.
   *
   * @param cb   - criteria builder to build orders
   * @param root - root path of entity
   * @param sortAttributes   - the list of attribute(s) to use for sorting. For reverse sort use dash(-) prefix on the attribute.
   * @param caseSensitive - Should order by on text fields be case sensitive or no ?
   * @return a list of {@link Order} from a given {@link CriteriaBuilder} and {@link Path}. Empty list or sortAttributes is empty.
   * @throws UnknownAttributeException if an attribute used in the sortAttributes list is unknown
   */
  public static <T> List<Order> getOrders(CriteriaBuilder cb, Path<T> root,
                                          List<String> sortAttributes, boolean caseSensitive)
      throws UnknownAttributeException {

    if (CollectionUtils.isEmpty(sortAttributes)) {
      return List.of();
    }

    List<Order> orderByClause = new ArrayList<>(sortAttributes.size());

    for (String sortAttribute : sortAttributes) {
      Expression<?> orderByExpression;
      Path<T> from = root;
      try {
        for (String path : StringUtils.split(StringUtils.removeStart(sortAttribute, REVERSE_ORDER_PREFIX), ".")) {
          from = from.get(path);
        }
      } catch (IllegalArgumentException iaEx) {
        //  if attribute of the given name does not exist
        throw new UnknownAttributeException(iaEx);
      }

      if (!caseSensitive && from.getJavaType() == String.class) {
        orderByExpression = cb.lower(from.as(String.class));
      } else {
        orderByExpression = from;
      }

      if (sortAttribute.startsWith(REVERSE_ORDER_PREFIX)) {
        orderByClause.add(cb.desc(orderByExpression));
      } else {
        orderByClause.add(cb.asc(orderByExpression));
      }

    }
    return orderByClause;
  }

  /**
   * Parses an attribute path starting from the given resourceClass, searching for mappable relationships.
   *
   * This method iterates through each part of the attribute path and checks if the relationship can be found
   * within the given resourceClass. If a part of the path cannot be found, an empty list is returned.
   *
   * External relationships and relationships that cannot be found will be ignored.
   *
   * @param registry      The DinaMappingRegistry used for mapping information.
   * @param resourceClass The resourceClass from where to start the search for the attribute.
   * @param attributePath The attribute path to parse, e.g., ("department", "employees") -> department.employees.
   * @return A List containing mappable attribute paths in the order they appear in attributePath.
   */
  private static List<String> parseMappableRelationshipPath(
    @NonNull DinaMappingRegistry registry,
    @NonNull Class<?> resourceClass,
    @NonNull List<String> attributePath) {
    List<String> fullPath = new ArrayList<>(attributePath.size());
    Class<?> dtoClass = resourceClass;

    for (String attr : attributePath) {
      if (hasMappableRelation(registry, dtoClass, attr)) {
        fullPath.add(attr);
        dtoClass = registry.getInternalRelationClass(dtoClass, attr);
      } else {
        // Internal relationship not found. Return an empty path.
        log.debug(
          "The attribute path '{}' cannot be found on the '{}' resource. Please ensure that the attribute is present on the resource and it's not pointing to an external relationship. The include will be ignored.",
          () -> String.join(".", attributePath), resourceClass::getCanonicalName);
        return List.of();
      }
    }
    return fullPath;
  }

  /**
   * Checks if a given class has a specified relation.
   *
   * This method determines whether the provided class has a relation with the specified name.
   * It uses the DinaMappingRegistry to find mappable relations for the given class and compares the
   * relation names in a case-insensitive manner with the provided relation name.
   *
   * @param registry The DinaMappingRegistry used for finding mappable relations.
   * @param aClass   The class to be evaluated for the specified relation.
   * @param relation The name of the relation to be checked in the class.
   *
   * @return {@code true} if the class has the specified relation; otherwise, {@code false}.
   */
  private static boolean hasMappableRelation(DinaMappingRegistry registry, Class<?> aClass, String relation) {
    return registry.findMappableRelationsForClass(aClass)
      .stream().anyMatch(rel -> rel.getName().equalsIgnoreCase(relation));
  }


  /**
   * Joins multiple attribute paths in a Root object using LEFT JOIN fetches.
   *
   * This method is used to join multiple attribute paths in a Root object using LEFT JOIN fetches.
   * The attribute paths are provided as a List of Strings, and the method performs successive LEFT JOIN
   * fetches for each attribute path in the list.
   *
   * @param root          The Root object to which the attribute paths are to be joined.
   * @param attributePath A List of Strings representing the attribute paths to be joined.
   */
  private static void joinAttributePath(Root<?> root, List<String> attributePath) {
    if (root == null || CollectionUtils.isEmpty(attributePath)) {
      return;
    }
    FetchParent<?, ?> join = root;
    for (String path : attributePath) {
      join = join.fetch(path, JoinType.LEFT);
    }
  }

}
