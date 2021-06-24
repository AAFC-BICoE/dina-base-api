package ca.gc.aafc.dina.testsupport.specs;

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

class MandatoryFieldValidator extends BaseJsonValidator<OAI3> {

  private static final ValidationResult MISSING_FIELD_ERROR
    = new ValidationResult(ERROR, 1026, "Field '%s' is required.");
  private static final ValidationResults.CrumbInfo CRUMB_MISSING_FIELD =
    new ValidationResults.CrumbInfo(REQUIRED, true);
  private static final ValidationResult ADDITIONAL_FIELD_ERROR
    = new ValidationResult(ERROR, 1000, "Additional property '%s' is not allowed.");
  private static final ValidationResults.CrumbInfo ADDITIONAL_FIELD_CRUMB
    = new ValidationResults.CrumbInfo(ADDITIONALPROPERTIES, true);

  private final Set<String> requiredAttributes = new HashSet<>();

  protected MandatoryFieldValidator(
    ValidationContext<OAI3> context,
    JsonNode schemaNode,
    JsonNode schemaParentNode,
    SchemaValidator parentSchema
  ) {
    super(context, schemaNode, schemaParentNode, parentSchema);
    schemaNode.fieldNames().forEachRemaining(s -> {
      if (s.equalsIgnoreCase("attributes")) {
        JsonNode attributesNode = schemaNode.at("/attributes/" + OAI3SchemaKeywords.PROPERTIES);
        attributesNode.fieldNames().forEachRemaining(requiredAttributes::add);
      }
    });
  }

  @Override
  public boolean validate(JsonNode valueNode, ValidationData<?> validation) {
    if (!requiredAttributes.isEmpty()) {
      valueNode.at("/attributes").fieldNames().forEachRemaining(field -> {
        if (requiredAttributes.stream().noneMatch(attrib -> attrib.equalsIgnoreCase(field))) {
          validation.add(ADDITIONAL_FIELD_CRUMB, ADDITIONAL_FIELD_ERROR, field);
        }
      });

      for (String fieldName : requiredAttributes) {
        if (valueNode.at("/attributes/" + fieldName).isMissingNode()) {
          validation.add(CRUMB_MISSING_FIELD, MISSING_FIELD_ERROR, fieldName);
        }
      }
    }
    return true;
  }
}
