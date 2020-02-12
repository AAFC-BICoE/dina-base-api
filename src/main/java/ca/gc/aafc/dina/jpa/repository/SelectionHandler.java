package ca.gc.aafc.dina.jpa.repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Named;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.From;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Selection;

import io.crnk.core.engine.information.resource.ResourceField;
import io.crnk.core.engine.information.resource.ResourceInformation;
import io.crnk.core.engine.internal.utils.PropertyUtils;
import io.crnk.core.engine.registry.ResourceRegistry;
import io.crnk.core.queryspec.IncludeFieldSpec;
import io.crnk.core.queryspec.IncludeRelationSpec;
import io.crnk.core.queryspec.QuerySpec;

/**
 * Provides methods for handling sparse field sets and inclusion of related resources.
 */
@Named
public class SelectionHandler {
  
  /**
   * Gets the selected fields as attribute paths from the querySpec.
   * 
   * @param querySpec
   * @param root
   * @return
   */
  private Map<Class<?>, List<List<String>>> getSelectedFieldsPerClass(
      ResourceRegistry resourceRegistry, QuerySpec querySpec) {
    Map<Class<?>, List<List<String>>> selectedFields = new HashMap<>();
    
    List<List<String>> selectedFieldsOfThisClass = new ArrayList<>();
    ResourceInformation resourceInformation = resourceRegistry
        .getEntry(querySpec.getResourceClass())
        .getResourceInformation();
    
    // If no fields are specified, include all fields.
    if (querySpec.getIncludedFields().size() == 0) {
      // Add the attribute fields
      selectedFieldsOfThisClass.addAll(
          resourceInformation.getAttributeFields()
              .stream()
              .map(ResourceField::getUnderlyingName)
              .map(Collections::singletonList)
              .collect(Collectors.toList())
      );
      // Add the ID fields for to-one relationships.
      selectedFieldsOfThisClass.addAll(
          resourceInformation.getRelationshipFields()
              .stream()
              .filter(field -> !field.isCollection())
              // Map each ResourceField to the attribute path of the related resource's ID, e.g.
              // ["region","id"].
              .map(field -> {
                List<String> relationIdPath = new ArrayList<>();
                // Add the field name to the attribute path e.g. "region"
                relationIdPath.add(field.getUnderlyingName());
                // Add the ID field name to the attribute path e.g. "id"
                relationIdPath.add(
                    resourceRegistry.findEntry(field.getElementType())
                        .getResourceInformation()
                        .getIdField()
                        .getUnderlyingName()
                );
                // Return the attribute path, e.g. ["region","id"].
                return relationIdPath;
              })
              .collect(Collectors.toList())
      );
    } else {
      for (IncludeFieldSpec includedField : querySpec.getIncludedFields()) {
        selectedFieldsOfThisClass.add(includedField.getAttributePath());
      }
    }

    // The id field is always selected, even if not explicitly requested by the user.
    String idAttribute = getIdAttribute(querySpec.getResourceClass(), resourceRegistry);
    if (!selectedFieldsOfThisClass.contains(Collections.singletonList(idAttribute))) {
      selectedFieldsOfThisClass.add(Collections.singletonList(idAttribute));
    }
    
    selectedFields.put(querySpec.getResourceClass(), selectedFieldsOfThisClass);
    
    // Add the selected fields for includes where sparse fields are requested.
    for (QuerySpec nestedSpec : querySpec.getNestedSpecs()) {
      selectedFields.putAll(this.getSelectedFieldsPerClass(resourceRegistry, nestedSpec));
    }
    
    // Add the selected fields for includes where fields are not selected for that type.
    for (IncludeRelationSpec rel : querySpec.getIncludedRelations()) {
      Class<?> relationClass = this.getPropertyClass(
          querySpec.getResourceClass(),
          rel.getAttributePath()
      );
      if (!selectedFields.containsKey(relationClass)) {
        selectedFields
            .putAll(this.getSelectedFieldsPerClass(resourceRegistry, new QuerySpec(relationClass)));
      }
    }
    
    return selectedFields;
  }
  
  public List<Selection<?>> getSelections(
      QuerySpec querySpec,
      From<?,?> root,
      ResourceRegistry resourceRegistry
  ) {

    Map<Class<?>, List<List<String>>> selectedFields = this
        .getSelectedFieldsPerClass(resourceRegistry, querySpec);
    
    List<Selection<?>> selections = new ArrayList<>();
    
    // Get the selections from the root resource.
    for (List<String> selectedAttributePath : selectedFields.get(querySpec.getResourceClass())) {
      // Each field selection is aliased by a period-separated path, e.g. "region.name".
      String alias = String.join(".", selectedAttributePath);
      
      selections.add(
          this.getExpression(root, selectedAttributePath)
              .alias(alias)
      );
    }
    
    // Loop through the "include"d relation paths to get the Selections for each relation.
    for (IncludeRelationSpec rel : querySpec.getIncludedRelations()) {
      List<Selection<?>> includeSelections = new ArrayList<>();
      for (List<String> selectedField : selectedFields.get(
          this.getPropertyClass(querySpec.getResourceClass(), rel.getAttributePath())
      )) {
        List<String> fieldPath = Stream
            .concat(rel.getAttributePath().stream(), selectedField.stream())
            .collect(Collectors.toList());
        includeSelections.add(
            this.getExpression(
                root,
                fieldPath
            )
            .alias(String.join(".", fieldPath))
        );
      }
      selections.addAll(includeSelections);
      
    }

    // There may be duplicate selections, which would cause a JPA error when the criteria query is
    // executed, so remove them here.
    selections = this.uniquelyAliasedSelections(selections);
    
    return selections;
  }
  
  /**
   * Gets a JPA expression given a base path and an attributePath. Works as a short-hand method to
   * get expressions that could require joins.
   * This method could be rewritten later to map DTO fields to custom expressions.
   * 
   * @param basePath the base path
   * @param attributePath the attribute path
   * @return the expression
   */
  public Expression<?> getExpression(From<?, ?> basePath, List<String> attributePath) {
    From<?, ?> from = basePath;
    for (String pathElement : attributePath.subList(0, attributePath.size() - 1)) {
      from = from.join(pathElement, JoinType.LEFT);
    }
    return from.get(attributePath.get(attributePath.size() - 1));
  }
  
  /**
   * Gets the name of the JPA entity's @Id attribute
   * 
   * @return
   */
  public String getIdAttribute(Class<?> resourceClass, ResourceRegistry resourceRegistry) {
    return resourceRegistry.findEntry(resourceClass)
        .getResourceInformation()
        .getIdField()
        .getUnderlyingName();
  }
  
  /**
   * Gets the resource class' @JsonApiId attribute.
   * 
   * @return the JPA Expression of the Id attribute.
   */
  public Expression<?> getIdExpression(
      From<?, ?> root,
      Class<?> resourceClass,
      ResourceRegistry resourceRegistry
  ) {
    return root.get(this.getIdAttribute(resourceClass, resourceRegistry));
  }
  
  /**
   * Get the class of a property that may be more than one element away.
   * 
   * @param baseType
   * @param attributePath
   * @return
   */
  private Class<?> getPropertyClass(Class<?> baseType, List<String> attributePath) {
    Class<?> type = baseType;
    for (String pathElement : attributePath) {
      type = PropertyUtils.getPropertyClass(type, pathElement);
    }
    return type;
  }
  
  /**
   * Get a unique list of Selections given a list that may include duplicates. Duplicates are found
   * when multiple selections are using the same alias. Aliases are named after property paths, e.g.
   * "name" or "region.id".
   * 
   * @param selections
   * @return
   */
  private List<Selection<?>> uniquelyAliasedSelections(List<Selection<?>> selections) {
    List<Selection<?>> uniqueSelections = new ArrayList<>();
    for (Selection<?> selection : selections) {
      if (
          !uniqueSelections.stream()
              .anyMatch(selection2 -> selection.getAlias().equals(selection2.getAlias()))
      ) {
        uniqueSelections.add(selection);
      }
    }
    return uniqueSelections;
  }
  
}
