package ca.gc.aafc.dina.jpa;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import org.springframework.beans.BeanUtils;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.SpelParserConfiguration;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import ca.gc.aafc.dina.jpa.repository.SelectionHandler;
import io.crnk.core.engine.information.resource.ResourceField;
import io.crnk.core.engine.information.resource.ResourceInformation;
import io.crnk.core.engine.internal.utils.PropertyUtils;
import io.crnk.core.engine.registry.ResourceRegistry;
import io.crnk.core.queryspec.IncludeFieldSpec;
import io.crnk.core.queryspec.IncludeRelationSpec;
import io.crnk.core.queryspec.QuerySpec;
import lombok.NonNull;

/**
 * Maps DTOs to JPA entities.
 */
public class JpaDtoMapper {
  
  private final BiMap<Class<?>, Class<?>> jpaEntities;
  private final SelectionHandler selectionHandler;
  
  public JpaDtoMapper(
    @NonNull Map<Class<?>, Class<?>> jpaEntities,
    @NonNull SelectionHandler selectionHandler
  ) {
    this.jpaEntities = HashBiMap.create(jpaEntities);
    this.selectionHandler = selectionHandler;
  }
  
  public Class<?> getEntityClassForDto(Class<?> dtoClass) {
    return jpaEntities.get(dtoClass);
  }
  
  public Class<?> getDtoClassForEntity(Class<?> entityClass) {
    return jpaEntities.inverse().get(entityClass);
  }
  
  /**
   * Converts an Entity to a DTO based on the selected fields in the QuerySpec.
   * 
   * @param entity
   * @param querySpec
   * @param resourceRegistry
   * @return the DTO
   */
  public Object toDto(Object entity, QuerySpec querySpec, ResourceRegistry resourceRegistry) {
    Class<?> dtoClass = this.getDtoClassForEntity(entity.getClass());

    Map<Class<?>, List<String>> selectedFieldsPerClass = getSelectedFieldsPerClass(resourceRegistry, querySpec);

    List<String> selectedFields = selectedFieldsPerClass.get(dtoClass);
    for (IncludeRelationSpec relation : querySpec.getIncludedRelations()) {
      Class<?> relationClass = getPropertyClass(dtoClass, relation.getAttributePath());
      List<String> relationSelectedFields = selectedFieldsPerClass.get(relationClass);
      List<String> fullFieldPaths = relationSelectedFields.stream()
        .map(path -> String.join(".", relation.getAttributePath()) + "." + path)
        .collect(Collectors.toList());
      selectedFields.addAll(fullFieldPaths);
    }

    Object dto = BeanUtils.instantiate(dtoClass);

    ExpressionParser entityParser = new SpelExpressionParser(
      // Auto-grow must be false for the entity to avoid adding empty related entities:
      new SpelParserConfiguration(false, false)
      );
      ExpressionParser dtoParser = new SpelExpressionParser(
      // Auto-grow must be true for the DTO instantiate related DTOs:
      new SpelParserConfiguration(true, true)
    );
    StandardEvaluationContext entityContext = new StandardEvaluationContext(entity);
    StandardEvaluationContext dtoContext = new StandardEvaluationContext(dto);

    for (String field : selectedFields) {
      Object value;
      try {
        value = entityParser.parseExpression(field).getValue(entityContext);
      } catch (EvaluationException ee) {
        value = null;
      }
      if (value != null) {
        dtoParser.parseExpression(field).setValue(dtoContext, value);
      }
    }

    return dto;
  }

  /**
   * Gets the selected fields as attribute paths from the querySpec.
   * 
   * @param querySpec
   * @param root
   * @return
   */
  private Map<Class<?>, List<String>> getSelectedFieldsPerClass(
      ResourceRegistry resourceRegistry, QuerySpec querySpec) {
    Map<Class<?>, List<String>> selectedFields = new HashMap<>();
    
    List<String> selectedFieldsOfThisClass = new ArrayList<>();
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
              .collect(Collectors.toList())
      );
      // Add the ID fields for to-one relationships.
      selectedFieldsOfThisClass.addAll(
          resourceInformation.getRelationshipFields()
              .stream()
              .filter(field -> !field.isCollection())
              // Map each ResourceField to the attribute path of the related resource's ID, e.g.
              // "region.id".
              .map(field -> {
                // Add the field name to the attribute path e.g. "region"
                String prefix = field.getUnderlyingName();
                // Add the ID field name to the attribute path e.g. "id"
                String suffix = resourceRegistry.findEntry(field.getElementType())
                    .getResourceInformation()
                    .getIdField()
                    .getUnderlyingName();
                // Return the attribute path, e.g. "region.id".
                return prefix + "." + suffix;
              })
              .collect(Collectors.toList())
      );
    } else {
      for (IncludeFieldSpec includedField : querySpec.getIncludedFields()) {
        selectedFieldsOfThisClass.add(String.join(".", includedField.getAttributePath()));
      }
    }

    // The id field is always selected, even if not explicitly requested by the user.
    String idAttribute = selectionHandler.getIdAttribute(
      querySpec.getResourceClass(),
      resourceRegistry
    );
    if (!selectedFieldsOfThisClass.contains(idAttribute)) {
      selectedFieldsOfThisClass.add(idAttribute);
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

}
