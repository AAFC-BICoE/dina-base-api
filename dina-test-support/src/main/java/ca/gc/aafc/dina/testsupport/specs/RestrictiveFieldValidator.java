package ca.gc.aafc.dina.testsupport.specs;

import com.fasterxml.jackson.databind.JsonNode;
import org.openapi4j.core.model.v3.OAI3;
import org.openapi4j.core.validation.ValidationResult;
import org.openapi4j.core.validation.ValidationResults;
import org.openapi4j.schema.validator.BaseJsonValidator;
import org.openapi4j.schema.validator.ValidationContext;
import org.openapi4j.schema.validator.ValidationData;
import org.openapi4j.schema.validator.v3.SchemaValidator;

import static org.openapi4j.core.model.v3.OAI3SchemaKeywords.ADDITIONALPROPERTIES;
import static org.openapi4j.core.model.v3.OAI3SchemaKeywords.REQUIRED;
import static org.openapi4j.core.validation.ValidationSeverity.ERROR;

/**
 * Used for Open api response validation. Given {@link ValidationRestrictionOptions} to set if additional
 * fields are allowed and to specify which fields may remain missing. Default validation Ensures all
 * attributes and relations are present in the response and does not allow additional fields.
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

  /**
   * Compares provided API response/request against schema specifications.
   * 
   * OpenAPI4j will validate each level independently. We need to perform the check on each level.
   * 
   * The schemaNode will also automatically update to the level we are currently at.
   * 
   * This method will scan one level at a time. If a nested object is found, it will run this method
   * again with a new schemaPath and valuePath based on the new level. Any fields in the
   * allowableMissingFields with nested objects will not be scanned any further.
   * 
   * This validator will consider all fields provided in the schema as "required". Even though it is
   * possible for some of these fields to not be provided. This is by design to ensure that new
   * fields are validated against the schema as they are added.
   * 
   * Couple of options can be provided to this validator (from the constructor):
   *    setAllowableMissingFields:
   *        List of string names of the fields you would like to ignore from the required
   *        field check. This also works for relationships.
   *    allowAdditionalFields:
   *        Default is false. When true, fields that are not on the schema will not cause a 
   *        validation error.
   * 
   * The schemaPath will contain /properties in its path while the valuePath will not.
   */
  @Override
  public boolean validate(JsonNode valueNode, ValidationData<?> validation) {
    // If we are at a level that contains attributes or data nodes then we can skip it.
    if (valueNode.has("attributes") || valueNode.has("data")) {
      return true;
    }

    // Retrieved all fields at this level in the valueNode.
    getSchemaNode().fieldNames().forEachRemaining(fieldName -> {
      // Does the value node provided contain this field name?
      if (!valueNode.has(fieldName) && !options.getAllowableMissingFields().contains(fieldName)) {
        // Report a missing schema value.
        validation.add(CRUMB_MISSING_FIELD, MISSING_FIELD_ERROR, fieldName);
      }
    });

    // If allow additional fields is true, then we can ignore the required and additional field checks.
    if (!options.isAllowAdditionalFields()) {
      valueNode.fieldNames().forEachRemaining(fieldName -> {
        // Does the schema contain this field, or is it an additional field.
        if (!getSchemaNode().has(fieldName) && !options.getAllowableMissingFields().contains(fieldName)) {
          // Report an additional schema value.
          validation.add(ADDITIONAL_FIELD_CRUMB, ADDITIONAL_FIELD_ERROR, fieldName);
        }
      });
    }

    return true;
  }
}
