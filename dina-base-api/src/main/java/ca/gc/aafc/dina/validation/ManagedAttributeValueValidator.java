package ca.gc.aafc.dina.validation;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.GenericTypeResolver;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import ca.gc.aafc.dina.entity.ManagedAttribute;
import ca.gc.aafc.dina.entity.ManagedAttribute.ManagedAttributeType;
import ca.gc.aafc.dina.service.ManagedAttributeService;
import lombok.NonNull;

import javax.inject.Named;

public class ManagedAttributeValueValidator<E extends ManagedAttribute> implements Validator {

  private final ManagedAttributeService<E> dinaService;
  private final MessageSource messageSource;

  private static final String VALID_ASSIGNED_VALUE = "assignedValue.invalid";
  private static final String VALID_ASSIGNED_VALUE_KEY = "assignedValue.key.invalid";
  private static final Pattern INTEGER_PATTERN = Pattern.compile("\\d+");

  public ManagedAttributeValueValidator(
      @Named("validationMessageSource") MessageSource messageSource,
      @NonNull ManagedAttributeService<E> dinaService) {
    this.messageSource = messageSource;
    this.dinaService = dinaService;
  }

  @Override
  public boolean supports(@NonNull Class<?> clazz) {
    return Map.class.isAssignableFrom(clazz);
  }

  @Override
  @SuppressWarnings("unchecked")
  public void validate(@NonNull Object target, @NonNull Errors errors) {
    if (!supports(target.getClass())) {
      throw new IllegalArgumentException("this validator can only validate the type: " + Map.class.getSimpleName());
    }

    ((Map<String, String>) target).forEach((key, assignedValue) -> {

      Class<E> clazz = (Class<E>) GenericTypeResolver.resolveTypeArgument(dinaService.getClass(), ManagedAttributeService.class);

      List<E> maList = dinaService.findByProperty(clazz, "key", key);
      if (maList.isEmpty()) {
        String errorMessage = messageSource.getMessage(VALID_ASSIGNED_VALUE_KEY, null,
          LocaleContextHolder.getLocale());
          errors.reject(errorMessage);
          return;
        }

      E ma = maList.get(0);

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
        String errorMessage = messageSource.getMessage(VALID_ASSIGNED_VALUE, new String[] { assignedValue },
            LocaleContextHolder.getLocale());
        errors.reject(errorMessage);
      }
    });
  }
  
}
