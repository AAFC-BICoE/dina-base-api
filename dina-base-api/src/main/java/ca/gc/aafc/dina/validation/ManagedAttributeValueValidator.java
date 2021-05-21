package ca.gc.aafc.dina.validation;

import ca.gc.aafc.dina.entity.ManagedAttribute;
import ca.gc.aafc.dina.entity.ManagedAttribute.ManagedAttributeType;
import ca.gc.aafc.dina.service.ManagedAttributeService;
import lombok.NonNull;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import javax.inject.Named;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Predicate;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ManagedAttributeValueValidator<E extends ManagedAttribute> implements Validator {

  private static final String KEY = "key";
  private final ManagedAttributeService<E> dinaService;
  private final MessageSource messageSource;

  private static final String VALID_ASSIGNED_VALUE = "assignedValue.invalid";
  private static final String VALID_ASSIGNED_VALUE_KEY = "assignedValue.key.invalid";
  private static final Pattern INTEGER_PATTERN = Pattern.compile("\\d+");
  private final Class<E> maClass;

  public ManagedAttributeValueValidator(
    @Named("validationMessageSource") MessageSource messageSource,
    @NonNull ManagedAttributeService<E> dinaService,
    @NonNull Class<E> managedAttributeClass
  ) {
    this.messageSource = messageSource;
    this.dinaService = dinaService;
    this.maClass = managedAttributeClass;
  }

  @Override
  public boolean supports(@NonNull Class<?> clazz) {
    return Map.class.isAssignableFrom(clazz);
  }

  @Override
  public void validate(@NonNull Object target, @NonNull Errors errors) {
    if (!supports(target.getClass())) {
      throw new IllegalArgumentException("this validator can only validate the type: " + Map.class.getSimpleName());
    }
    Map<String, String> map = (Map<String, String>) target;
    Map<String, E> attributesPerKey = findAttributesForKeys(map.keySet(), maClass);

    Collection<?> difference = CollectionUtils.disjunction(map.keySet(), attributesPerKey.keySet());
    if (!difference.isEmpty()) {
      errors.reject(getMessageForKey(VALID_ASSIGNED_VALUE_KEY));
      return;
    }

    attributesPerKey.forEach((key, ma) -> {
      ManagedAttributeType maType = ma.getManagedAttributeType();
      String assignedValue = map.get(key);

      if (maType == ManagedAttributeType.INTEGER && !INTEGER_PATTERN.matcher(assignedValue).matches()) {
        errors.reject(getMessageForKey(VALID_ASSIGNED_VALUE), assignedValue);
        return;
      }

      Set<String> acceptedValues = Arrays.stream(ma.getAcceptedValues()).collect(Collectors.toSet());
      if (CollectionUtils.isNotEmpty(acceptedValues)
        && acceptedValues.stream().noneMatch(assignedValue::equalsIgnoreCase)) {
        errors.reject(getMessageForKey(VALID_ASSIGNED_VALUE, assignedValue));
      }
    });
  }

  private Map<String, E> findAttributesForKeys(Set<String> keySet, Class<E> clazz) {
    if (CollectionUtils.isEmpty(keySet) || clazz == null) {
      return Map.of();
    }
    return dinaService.findAll(
      clazz, (criteriaBuilder, eRoot) -> {
        CriteriaBuilder.In<String> in = criteriaBuilder.in(eRoot.get(KEY));
        keySet.forEach(in::value);
        return new Predicate[]{in};
      },
      null, 0, Integer.MAX_VALUE
    ).stream().collect(Collectors.toMap(ManagedAttribute::getKey, Function.identity()));
  }

  private String getMessageForKey(String key, Object... objects) {
    return messageSource.getMessage(key, objects, LocaleContextHolder.getLocale());
  }

}
