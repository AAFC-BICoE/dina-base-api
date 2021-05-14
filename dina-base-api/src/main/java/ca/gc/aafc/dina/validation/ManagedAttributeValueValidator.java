package ca.gc.aafc.dina.validation;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.persistence.criteria.Predicate;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.GenericTypeResolver;
import org.springframework.core.ResolvableType;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import ca.gc.aafc.dina.entity.DinaEntity;
import ca.gc.aafc.dina.entity.ManagedAttribute;
import ca.gc.aafc.dina.entity.ManagedAttributeValue;
import ca.gc.aafc.dina.entity.ManagedAttribute.ManagedAttributeType;
import ca.gc.aafc.dina.service.DefaultDinaService;
import ca.gc.aafc.dina.service.ManagedAttributeService;
import lombok.NonNull;

@Component
public class ManagedAttributeValueValidator<E extends ManagedAttribute> implements Validator {

  private DefaultDinaService<E> dinaService;

  private final MessageSource messageSource;

  private static final String VALID_ASSIGNED_VALUE = "assignedValue.invalid";
  private static final Pattern INTEGER_PATTERN = Pattern.compile("\\d+");

  public ManagedAttributeValueValidator(
    MessageSource messageSource, 
    @NonNull DefaultDinaService<E> dinaService) {
    this.messageSource = messageSource;
    this.dinaService = dinaService;
  }
  @Override
  public boolean supports(Class<?> clazz) {
    return Entry.class.isAssignableFrom(clazz);
  }

  @Override
  public void validate(Object target, Errors errors) {
    Map.Entry<String,String> map = (Entry) target;

    String key = map.getKey();
    String assignedValue = map.getValue();
    ResolvableType resolvableType = ResolvableType.forClass(ManagedAttributeValueValidator.class);

    ResolvableType type = resolvableType.getGeneric().getGeneric();
    Class<E> clazz = (Class<E>) type.resolve();
    //Class<E> clazz = (Class<E>) GenericTypeResolver.resolveTypeArgument(getClass(), ManagedAttributeValueValidator.class);

    E ma = dinaService.findOneByProperty(clazz, "key", key);
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
      errors.rejectValue("assignedValue", VALID_ASSIGNED_VALUE, new String[] { assignedValue }, errorMessage);
    }
  }
  
}
