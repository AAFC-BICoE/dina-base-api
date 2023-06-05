package ca.gc.aafc.dina.validation;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.math.NumberUtils;

import ca.gc.aafc.dina.vocabulary.TypedVocabularyElement;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.regex.Pattern;
import lombok.NonNull;

/**
 * Package protected utility class to validate assigned values for TypedVocabularyElement.
 * Can be used with managed attributes and field extension.
 */
public final class TypedVocabularyElementValidator {

  private TypedVocabularyElementValidator() {
    // utility class
  }

  private static final Pattern INTEGER_PATTERN = Pattern.compile("\\d+");

  static boolean isAcceptedValue(@NonNull String assignedValue, String[] acceptedValues) {
    return isAcceptedValue(assignedValue, acceptedValues, true);
  }

  static boolean isAcceptedValue(@NonNull String assignedValue, String[] acceptedValues,
                                 boolean ignoreCase) {
    return Arrays.stream(acceptedValues)
      .anyMatch(ignoreCase ? assignedValue::equalsIgnoreCase : assignedValue::equals);
  }

  /**
   * Checks if the assignedValue is valid for the {@link TypedVocabularyElement} in terms of type {@link TypedVocabularyElement.VocabularyElementType}.
   * Valid means the assignedValue can be represented/parsed in the given type.
   * Accepted values are NOT in scope for that method, {@link #isAcceptedValue(String, String[], boolean)} should be used.
   * @param tvElement
   * @param assignedValue
   * @return
   */
  static boolean isValidElement(TypedVocabularyElement tvElement, String assignedValue) {
    TypedVocabularyElement.VocabularyElementType tvType = tvElement.getVocabularyElementType();
    switch(tvType) {
      case DATE :
        if (!isValidLocalDate(assignedValue)) {
          return false;
        }
        break;
      case INTEGER:
        if (!INTEGER_PATTERN.matcher(assignedValue).matches()) {
          return false;
        }
        break;
      case BOOL:
        if (!isValidBool(assignedValue)) {
          return false;
        }
        break;
      case DECIMAL:
        if(!NumberUtils.isParsable(assignedValue)) {
          return false;
        }
        break;
      case STRING:
        return true;
      default: // unknown type
        return false;
    }
    return true;
  }

  static boolean isValidLocalDate(String assignedValue) {
    try {
      LocalDate.parse(assignedValue);
    } catch (DateTimeParseException e) {
      return false;
    }
    return true;
  }

  static boolean isValidBool(String assignedValue) {
    return BooleanUtils.TRUE.equals(assignedValue) || BooleanUtils.FALSE.equals(assignedValue);
  }
}
