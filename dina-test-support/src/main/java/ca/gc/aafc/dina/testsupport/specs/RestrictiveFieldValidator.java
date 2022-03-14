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

import lombok.extern.log4j.Log4j2;

import java.util.HashSet;
import java.util.Set;

import static org.openapi4j.core.model.v3.OAI3SchemaKeywords.ADDITIONALPROPERTIES;
import static org.openapi4j.core.model.v3.OAI3SchemaKeywords.REQUIRED;
import static org.openapi4j.core.validation.ValidationSeverity.ERROR;

/**
 * Used for Open api response validation. Given {@link ValidationRestrictionOptions} to set if additional
 * fields are allowed and to specify which fields may remain missing. Default validation Ensures all
 * attributes and relations are present in the response and does not allow additional fields.
 */
@Log4j2
class RestrictiveFieldValidator extends BaseJsonValidator<OAI3> {

  private static final ValidationResult MISSING_FIELD_ERROR =
    new ValidationResult(ERROR, 1026, "Field '%s' is required.");
  private static final ValidationResults.CrumbInfo CRUMB_MISSING_FIELD =
    new ValidationResults.CrumbInfo(REQUIRED, true);
  private static final ValidationResult ADDITIONAL_FIELD_ERROR =
    new ValidationResult(ERROR, 1000, "Additional property '%s' is not allowed.");
  private static final ValidationResults.CrumbInfo ADDITIONAL_FIELD_CRUMB =
    new ValidationResults.CrumbInfo(ADDITIONALPROPERTIES, true);

  public static final String ATTRIBUTES_SCHEMA_PATH = "/data/properties/attributes";
  public static final String ATTRIBUTES_VALUE_PATH = "/data/attributes";

  public static final String RELATIONSHIPS_SCHEMA_PATH = "/data/properties/relationships";
  public static final String RELATIONSHIPS_VALUE_PATH = "/data/relationships";

  private final ValidationRestrictionOptions options;

  protected RestrictiveFieldValidator(
    ValidationContext<OAI3> context,
    JsonNode schemaNode,
    JsonNode schemaParentNode,
    SchemaValidator parentSchema,
    ValidationRestrictionOptions options
  ) {
    super(context, schemaNode, schemaParentNode, parentSchema);
    this.options = options == null ? ValidationRestrictionOptions.FULL_RESTRICTIONS : options;
  }

  @Override
  public boolean validate(JsonNode valueNode, ValidationData<?> validation) {
    validateFields(valueNode, validation, ATTRIBUTES_SCHEMA_PATH, ATTRIBUTES_VALUE_PATH);
    validateFields(valueNode, validation, RELATIONSHIPS_SCHEMA_PATH, RELATIONSHIPS_VALUE_PATH);
    return true;
  }

  private void validateFields(
    JsonNode valueNode,
    ValidationData<?> validationMessages,
    String schemaPath,
    String valuePath
  ) {

    // Retrieve all possible schema fields at this path.
    Set<String> possibleFields = new HashSet<>();
    this.getSchemaNode().at(schemaPath + "/properties").fieldNames().forEachRemaining(possibleFields::add);

    // Retrieve all required schema fields at this path.
    Set<String> requiredFields = new HashSet<>();
    JsonNode required = this.getSchemaNode().at(schemaPath + "/required");
    if (required.isArray()) {
      required.fieldNames().forEachRemaining(requiredFields::add);
    }

    // If allow additional fields is true, then we can ignore the required and additional field checks.
    if (!options.isAllowAdditionalFields()) {
      // Go through all of the required fields and ensure they are set.
      requiredFields.forEach(requiredFieldName -> {
        if (valueNode.at(valuePath).isMissingNode() && setDoesNotContainIgnoreCase(options.getAllowableMissingFields(), requiredFieldName)) {
          validationMessages.add(CRUMB_MISSING_FIELD, MISSING_FIELD_ERROR, requiredFieldName);
        }
      });

      // Go through each of the fields provided, and compare it against the schema.
      valueNode.at(valuePath).fieldNames().forEachRemaining(fieldName -> {
        if (setDoesNotContainIgnoreCase(possibleFields, fieldName) && setDoesNotContainIgnoreCase(options.getAllowableMissingFields(), fieldName)) {
          validationMessages.add(ADDITIONAL_FIELD_CRUMB, ADDITIONAL_FIELD_ERROR, fieldName);
        }
      });
    }

    // Check for any nested objects at this schema level.
    if (valuePath.contains("attributes")) {
      this.getSchemaNode().at(schemaPath + "/properties").fieldNames().forEachRemaining(fieldName -> {
        if (this.getSchemaNode().at(schemaPath + "/properties/" + fieldName).has("properties")) {
          // If found, we can now validate this nested object for required and additional fields.
          validateFields(valueNode, validationMessages, schemaPath + "/properties/" + fieldName, valuePath + "/" + fieldName);
        }
      });      
    }
  }

  private static boolean setDoesNotContainIgnoreCase(Set<String> requiredFieldNames, String field) {
    if (requiredFieldNames == null || requiredFieldNames.isEmpty() || field == null || field.isBlank()) {
      return true;
    }
    return requiredFieldNames.stream().noneMatch(attrib -> attrib.equalsIgnoreCase(field));
  }
}
