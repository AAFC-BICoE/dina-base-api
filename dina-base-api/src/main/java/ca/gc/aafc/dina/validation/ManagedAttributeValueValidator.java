package ca.gc.aafc.dina.validation;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
  private static final String VALID_MANAGED_ATTRIBUTE_TYPE = "validation.managedAttributeType.inconsistent";
  private static final Pattern INTEGER_PATTERN = Pattern.compile("\\d+");

  public ManagedAttributeValueValidator(
      @Named("validationMessageSource") MessageSource messageSource,
      @NonNull ManagedAttributeService<E> dinaService) {
    this.messageSource = messageSource;
    this.dinaService = dinaService;
  }

  @Override
  public boolean supports(Class<?> clazz) {
    return Entry.class.isAssignableFrom(clazz);
  }

  @Override
  @SuppressWarnings("unchecked")
  public void validate(Object target, Errors errors) {
    Map.Entry<String,String> map = (Entry) target;

    String key = map.getKey();
    String assignedValue = map.getValue();

    Class<E> clazz = (Class<E>) GenericTypeResolver.resolveTypeArgument(dinaService.getClass(), ManagedAttributeService.class);

    List<E> maList = dinaService.findByProperty(clazz, "key", key);
    if (maList.isEmpty()) {
      String errorMessage = messageSource.getMessage(VALID_ASSIGNED_VALUE_KEY, null,
        LocaleContextHolder.getLocale());
      throw new IllegalArgumentException(errorMessage);
    }

    Set<String> acceptedValues = new HashSet<>();
    Set<ManagedAttributeType> maType = new HashSet<>();
    for (E ma : maList) {
      List<String> acceptedValuesList = ma.getAcceptedValues() == null ? Collections.emptyList()
        : Arrays.stream(ma.getAcceptedValues()).map(String::toUpperCase).collect(Collectors.toList());
      acceptedValues.addAll(acceptedValuesList);
      maType.add(ma.getManagedAttributeType());
    }

    // Inconsistent ManagedAttributeType
    if (maType.size() > 1) {
      String errorMessage = messageSource.getMessage(VALID_MANAGED_ATTRIBUTE_TYPE, null,
        LocaleContextHolder.getLocale());
      throw new IllegalArgumentException(errorMessage);
    }
    
    
    boolean assignedValueIsValid = true;

    if (acceptedValues.isEmpty()) {
      if (maType.iterator().next() == ManagedAttributeType.INTEGER && !INTEGER_PATTERN.matcher(assignedValue).matches()) {
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
  }
  
}
