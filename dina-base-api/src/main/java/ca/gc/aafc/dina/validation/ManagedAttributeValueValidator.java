package ca.gc.aafc.dina.validation;

import ca.gc.aafc.dina.entity.ManagedAttribute;
import ca.gc.aafc.dina.entity.ManagedAttribute.ManagedAttributeType;
import ca.gc.aafc.dina.service.ManagedAttributeService;
import lombok.NonNull;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import javax.inject.Named;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ManagedAttributeValueValidator<E extends ManagedAttribute> implements Validator {

  private final ManagedAttributeService<E> dinaService;
  private final MessageSource messageSource;

  private static final String VALID_ASSIGNED_VALUE = "assignedValue.invalid";
  private static final String VALID_ASSIGNED_VALUE_KEY = "assignedValue.key.invalid";
  private static final Pattern INTEGER_PATTERN = Pattern.compile("\\d+");

  public ManagedAttributeValueValidator(
    @Named("validationMessageSource") MessageSource messageSource,
    @NonNull ManagedAttributeService<E> dinaService
  ) {
    this.messageSource = messageSource;
    this.dinaService = dinaService;
  }

  @Override
  public boolean supports(@NonNull Class<?> clazz) {
    return Map.class.isAssignableFrom(clazz);
  }

  @Override
  public void validate(@NonNull Object target, @NonNull Errors errors) {
    checkIncomingParameter(target);

    @SuppressWarnings("unchecked") // We check with checkIncomingParameter()
    final Map<String, String> map = (Map<String, String>) target;
    Map<String, E> attributesPerKey = dinaService.findAttributesForKeys(map.keySet());

    Collection<?> difference = CollectionUtils.disjunction(map.keySet(), attributesPerKey.keySet());
    if (!difference.isEmpty()) {
      errors.reject(getMessageForKey(VALID_ASSIGNED_VALUE_KEY));
    }

    attributesPerKey.forEach((key, ma) -> {
      ManagedAttributeType maType = ma.getManagedAttributeType();
      String assignedValue = map.get(key);

      if (maType == ManagedAttributeType.INTEGER && !INTEGER_PATTERN.matcher(assignedValue).matches()) {
        errors.reject(getMessageForKey(VALID_ASSIGNED_VALUE), assignedValue);
      }

      String[] acceptedValues = ma.getAcceptedValues();
      if (isNotAnAcceptedValue(assignedValue, acceptedValues)) {
        errors.reject(getMessageForKey(VALID_ASSIGNED_VALUE, assignedValue));
      }
    });
  }

  private void checkIncomingParameter(Object target) {
    if (!supports(target.getClass())) {
      throw new IllegalArgumentException("this validator can only validate the type: " + Map.class.getSimpleName());
    }
    ((Map<?, ?>) target).forEach((o, o2) -> {
      if (!String.class.isAssignableFrom(o.getClass()) || !String.class.isAssignableFrom(o2.getClass())) {
        throw new IllegalArgumentException(
          "This validator can only validate maps with keys and values as strings");
      }
    });
  }

  private static boolean isNotAnAcceptedValue(@NonNull String assignedValue, String[] acceptedValues) {
    return ArrayUtils.isNotEmpty(acceptedValues)
      && Arrays.stream(acceptedValues).collect(Collectors.toSet()).stream()
      .noneMatch(assignedValue::equalsIgnoreCase);
  }

  private String getMessageForKey(String key, Object... objects) {
    return messageSource.getMessage(key, objects, LocaleContextHolder.getLocale());
  }

}