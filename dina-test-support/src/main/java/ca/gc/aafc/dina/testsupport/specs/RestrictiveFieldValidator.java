package ca.gc.aafc.dina.testsupport.specs;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import org.openapi4j.core.model.v3.OAI3;
import org.openapi4j.core.model.v3.OAI3SchemaKeywords;
import org.openapi4j.core.validation.ValidationResult;
import org.openapi4j.core.validation.ValidationResults;
import org.openapi4j.schema.validator.BaseJsonValidator;
import org.openapi4j.schema.validator.ValidationContext;
import org.openapi4j.schema.validator.ValidationData;
import org.openapi4j.schema.validator.v3.SchemaValidator;

import java.util.HashSet;
import java.util.Set;

import static org.openapi4j.core.model.v3.OAI3SchemaKeywords.ADDITIONALPROPERTIES;
import static org.openapi4j.core.model.v3.OAI3SchemaKeywords.REQUIRED;
import static org.openapi4j.core.validation.ValidationSeverity.ERROR;

/**
 * Used for Open api response validation. Ensures all attributes and relations are present in the response and
 * does not allow additional fields.
 */
class RestrictiveFieldValidator extends BaseJsonValidator<OAI3> {

  private static final ValidationResult MISSING_FIELD_ERROR =
    new ValidationResult(ERROR, 1026, "Field '%s' is required.");
  private static final ValidationResults.CrumbInfo CRUMB_MISSING_FIELD =
    new ValidationResults.CrumbInfo(REQUIRED, true);
  private static final ValidationResult ADDITIONAL_FIELD_ERROR =
    new ValidationResult(ERROR, 1000, "Additional property '%s' is not allowed.");
  private static final ValidationResults.CrumbInfo ADDITIONAL_FIELD_CRUMB =
    new ValidationResults.CrumbInfo(ADDITIONALPROPERTIES, true);

  public static final String ATTRIBUTES_BLOCK_NAME = "attributes";
  public static final String RELATIONSHIPS_BLOCK_NAME = "relationships";

  public static final JsonPointer ATTRIB_POINTER =
    JsonPointer.compile("/" + ATTRIBUTES_BLOCK_NAME + "/" + OAI3SchemaKeywords.PROPERTIES);
  public static final JsonPointer RELATION_POINTER =
    JsonPointer.compile("/" + RELATIONSHIPS_BLOCK_NAME + "/" + OAI3SchemaKeywords.PROPERTIES);

  private final Set<String> requiredAttributes = new HashSet<>();
  private final Set<String> requiredRelations = new HashSet<>();
  private final ValidationRestrictionOptions options;

  protected RestrictiveFieldValidator(
    ValidationContext<OAI3> context,
    JsonNode schemaNode,
    JsonNode schemaParentNode,
    SchemaValidator parentSchema,
    ValidationRestrictionOptions options
  ) {
    super(context, schemaNode, schemaParentNode, parentSchema);
    this.options = options;
    schemaNode.fieldNames().forEachRemaining(node -> {
      if (node.equalsIgnoreCase(ATTRIBUTES_BLOCK_NAME)) {
        JsonNode attributesNode = schemaNode.at(ATTRIB_POINTER);
        attributesNode.fieldNames().forEachRemaining(requiredAttributes::add);
      }
      if (node.equalsIgnoreCase(RELATIONSHIPS_BLOCK_NAME)) {
        JsonNode attributesNode = schemaNode.at(RELATION_POINTER);
        attributesNode.fieldNames().forEachRemaining(requiredRelations::add);
      }
    });
  }

  @Override
  public boolean validate(JsonNode valueNode, ValidationData<?> validation) {
    validateRequiredFields(valueNode, validation, requiredAttributes, ATTRIBUTES_BLOCK_NAME);
    validateRequiredFields(valueNode, validation, requiredRelations, RELATIONSHIPS_BLOCK_NAME);
    return true;
  }

  private void validateRequiredFields(
    JsonNode valueNode,
    ValidationData<?> validation,
    Set<String> requiredFieldNames,
    String blockName
  ) {
    if (!requiredFieldNames.isEmpty()) {
      if (!options.isAllowAdditionalFields()) {
        checkAdditionalFields(valueNode, validation, requiredFieldNames, blockName);
      }
      checkMissingFields(valueNode, validation, requiredFieldNames, blockName);
    }
  }

  private void checkMissingFields(
    JsonNode valueNode,
    ValidationData<?> validation,
    Set<String> requiredFieldNames,
    String blockName
  ) {
    for (String fieldName : requiredFieldNames) {
      if (valueNode.at("/" + blockName + "/" + fieldName).isMissingNode()
        && setDoesNotContainIgnoreCase(options.getAllowableMissingFields(), fieldName)) {
        validation.add(CRUMB_MISSING_FIELD, MISSING_FIELD_ERROR, fieldName);
      }
    }
  }

  private static void checkAdditionalFields(
    JsonNode valueNode,
    ValidationData<?> validation,
    Set<String> requiredFieldNames,
    String blockName
  ) {
    valueNode.at("/" + blockName).fieldNames().forEachRemaining(field -> {
      if (setDoesNotContainIgnoreCase(requiredFieldNames, field)) {
        validation.add(ADDITIONAL_FIELD_CRUMB, ADDITIONAL_FIELD_ERROR, field);
      }
    });
  }

  private static boolean setDoesNotContainIgnoreCase(Set<String> requiredFieldNames, String field) {
    return requiredFieldNames.stream().noneMatch(attrib -> attrib.equalsIgnoreCase(field));
  }
}
