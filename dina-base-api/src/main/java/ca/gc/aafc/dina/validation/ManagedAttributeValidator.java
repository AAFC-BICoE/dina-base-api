package ca.gc.aafc.dina.validation;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import ca.gc.aafc.dina.entity.ManagedAttribute;
import ca.gc.aafc.dina.entity.ManagedAttribute.ManagedAttributeType;

@Component
public class ManagedAttributeValidator implements Validator {

  private final MessageSource messageSource;
  private static final Pattern INTEGER_PATTERN = Pattern.compile("\\d+");

  public ManagedAttributeValidator(MessageSource messageSource) {
    this.messageSource = messageSource;
  }
  @Override
  public boolean supports(Class<?> clazz) {
    return ManagedAttribute.class.isAssignableFrom(clazz);
  }

  @Override
  public void validate(Object target, Errors errors) {
    ManagedAttribute ma = (ManagedAttribute) target;
    List<String> acceptedValues = ma.getAcceptedValues() == null ? Collections.emptyList()
      : Arrays.stream(ma.getAcceptedValues()).map(String::toUpperCase).collect(Collectors.toList());
    
    ManagedAttributeType maType = ma.getManagedAttributeType();

    
    boolean assignedValueIsValid = true;

    // if (acceptedValues.isEmpty()) {
    //   if (maType == ManagedAttributeType.INTEGER && !INTEGER_PATTERN.matcher(assignedValue).matches()) {
    //     assignedValueIsValid = false;
    //   }
    // } else {
    //   if (!acceptedValues.contains(assignedValue.toUpperCase())) {
    //     assignedValueIsValid = false;
    //   }
    // }
    // if (!assignedValueIsValid) {
    //   String errorMessage = messageSource.getMessage("assignedValue.invalid", new String[] { assignedValue },
    //       LocaleContextHolder.getLocale());
    //   errors.rejectValue("assignedValue", "assignedValue.invalid", new String[] { assignedValue }, errorMessage);
    // }
  }
  
}
