package ca.gc.aafc.dina.testsupport.specs;

import com.networknt.schema.ExecutionContext;
import com.networknt.schema.Schema;
import com.networknt.schema.SchemaContext;
import com.networknt.schema.SchemaLocation;
import com.networknt.schema.keyword.Keyword;
import com.networknt.schema.keyword.KeywordValidator;
import com.networknt.schema.keyword.PropertiesValidator;
import com.networknt.schema.path.NodePath;
import tools.jackson.databind.JsonNode;

/**
 * Used for Open api response validation. Given {@link ValidationRestrictionOptions} to set if additional
 * fields are allowed and to specify which fields may remain missing. Default validation Ensures all
 * attributes and relations are present in the response and does not allow additional fields.
 *
 * It replaces the standard "properties" keyword: the standard validation is performed first and the
 * restrictive checks are added on top of it.
 */
class RestrictiveFieldValidator extends PropertiesValidator {

  private final ValidationRestrictionOptions options;

  RestrictiveFieldValidator(SchemaLocation schemaLocation, JsonNode schemaNode, Schema parentSchema,
                            SchemaContext schemaContext, ValidationRestrictionOptions options) {
    super(schemaLocation, schemaNode, parentSchema, schemaContext);
    this.options = options == null ? ValidationRestrictionOptions.FULL_RESTRICTIONS : options;
  }

  /**
   * Creates the "properties" {@link Keyword} to register in a dialect.
   */
  static Keyword keyword(ValidationRestrictionOptions options) {
    return new Keyword() {
      @Override
      public String getValue() {
        return PROPERTY;
      }

      @Override
      public KeywordValidator newValidator(SchemaLocation schemaLocation, JsonNode schemaNode,
                                           Schema parentSchema, SchemaContext schemaContext) {
        return new RestrictiveFieldValidator(schemaLocation, schemaNode, parentSchema, schemaContext, options);
      }
    };
  }

  /**
   * Compares provided API response/request against schema specifications.
   *
   * Each level is validated independently so the check is performed on each level. The schemaNode
   * is the "properties" node of the level we are currently at.
   *
   * This validator will consider all fields provided in the schema as "required". Even though it is
   * possible for some of these fields to not be provided. This is by design to ensure that new
   * fields are validated against the schema as they are added.
   *
   * Options:
   *    allowableMissingFields:
   *        Names of the fields to ignore from the required field check. This also works for
   *        relationships and fields nested under them.
   *    allowAdditionalFields:
   *        Default is false. When true, fields that are not on the schema will not cause a
   *        validation error.
   */
  @Override
  public void validate(ExecutionContext executionContext, JsonNode node, JsonNode rootNode,
                       NodePath instanceLocation) {
    super.validate(executionContext, node, rootNode, instanceLocation);

    if (!node.isObject()) {
      return;
    }

    // If we are at a level that contains attributes or data nodes then we can skip it.
    if (node.has("attributes") || node.has("data")) {
      return;
    }

    // If the path to this level goes through an allowable missing field, skip checking anything.
    for (int i = 0; i < instanceLocation.getNameCount(); i++) {
      if (isAllowableMissingField(instanceLocation.getName(i))) {
        return;
      }
    }

    getSchemaNode().propertyNames().forEach(fieldName -> {
      if (!node.has(fieldName) && !isAllowableMissingField(fieldName)) {
        executionContext.addError(error().keyword("required").messageKey("required")
          .instanceNode(node).property(fieldName).instanceLocation(instanceLocation)
          .evaluationPath(executionContext.getEvaluationPath())
          .locale(executionContext.getExecutionConfig().getLocale())
          .message("Field '" + fieldName + "' is required.").build());
      }
    });

    if (!options.isAllowAdditionalFields()) {
      node.propertyNames().forEach(fieldName -> {
        if (!getSchemaNode().has(fieldName) && !isAllowableMissingField(fieldName)) {
          executionContext.addError(error().keyword("additionalProperties").messageKey("additionalProperties")
            .instanceNode(node).property(fieldName).instanceLocation(instanceLocation.append(fieldName))
            .evaluationPath(executionContext.getEvaluationPath())
            .locale(executionContext.getExecutionConfig().getLocale())
            .message("Additional property '" + fieldName + "' is not allowed.").build());
        }
      });
    }
  }

  private boolean isAllowableMissingField(String fieldName) {
    return fieldName != null && options.getAllowableMissingFields() != null
      && options.getAllowableMissingFields().contains(fieldName);
  }
}
