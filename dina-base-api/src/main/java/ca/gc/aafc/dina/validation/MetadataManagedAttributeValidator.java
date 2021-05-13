package ca.gc.aafc.dina.validation;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import ca.gc.aafc.dina.entity.ManagedAttribute;
import ca.gc.aafc.dina.entity.MetadataManagedAttribute;
import ca.gc.aafc.dina.entity.ManagedAttribute.ManagedAttributeType;

@Component
public class MetadataManagedAttributeValidator implements Validator {

  private final MessageSource messageSource;
  private static final Pattern INTEGER_PATTERN = Pattern.compile("\\d+");

  public MetadataManagedAttributeValidator(MessageSource messageSource) {
    this.messageSource = messageSource;
  }
  @Override
  public boolean supports(Class<?> clazz) {
    return MetadataManagedAttribute.class.isAssignableFrom(clazz);
  }

  @Override
  public void validate(Object target, Errors errors) {
    MetadataManagedAttribute mav = (MetadataManagedAttribute) target;
    ManagedAttribute ma = mav.getManagedAttribute();
    String assignedValue = mav.getAssignedValue();
    List<String> acceptedValues = ma.getAcceptedValues() == null ? Collections.emptyList()
      : Arrays.stream(ma.getAcceptedValues()).map(String::toUpperCase).collect(Collectors.toList());
    
    ManagedAttributeType maType = ma.getManagedAttributeType();
    
    boolean assignedValueIsValid = true;

    if (acceptedValues.isEmpty()) {
      if (maType == ManagedAttributeType.INTEGER && !INTEGER_PATTERN.matcher(assignedValue).matches()) {
        assignedValueIsValid = false;
      }
    } else {
      if (!acceptedValues.contains(assignedValue.toUpperCase())) {
        assignedValueIsValid = false;
      }
    }
    if (!assignedValueIsValid) {
      String errorMessage = messageSource.getMessage("assignedValue.invalid", new String[] { assignedValue },
          LocaleContextHolder.getLocale());
      errors.rejectValue("assignedValue", "assignedValue.invalid", new String[] { assignedValue }, errorMessage);
    }
  }
  
}
