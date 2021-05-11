package ca.gc.aafc.dina.service;

import ca.gc.aafc.dina.entity.DinaEntity;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.ObjectError;
import org.springframework.validation.SmartValidator;
import org.springframework.validation.Validator;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.Constraint;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Validation;
import javax.validation.ValidationException;
import javax.validation.ValidatorFactory;
import javax.validation.executable.ExecutableValidator;

import com.google.common.collect.Sets;


@Component
public class ServiceValidationHelper {

  @Autowired
  private SmartValidator injectValidator;

  private static SmartValidator validator;

  @PostConstruct
  public void init() {
    ServiceValidationHelper.validator = injectValidator;
  }

  protected static <E extends DinaEntity> void callValidatedCreate(DefaultDinaService<E> dinaService, E entity) {

    Errors errors = new BeanPropertyBindingResult(entity,
        entity.getUuid() != null ? entity.getUuid().toString() : "");

    validator.validate(entity, errors, OnCreate.class);

    if (errors.hasErrors()) {

      Set<ConstraintViolation<E>> violations = new HashSet<>();

      for(ObjectError o : errors.getAllErrors()) {
        if (o.contains(ConstraintViolation.class)) { 
          violations.add((ConstraintViolation<E>) o.unwrap(ConstraintViolation.class));
        }
      }      

      throw new ConstraintViolationException(violations);
    }
  
    dinaService.validatedCreate(entity);
    
  }

  protected static <E extends DinaEntity> E callValidatedUpdate(DefaultDinaService<E> dinaService, E entity) {
    Errors errors = new BeanPropertyBindingResult(entity,
        entity.getUuid() != null ? entity.getUuid().toString() : "");

    validator.validate(entity, errors, OnUpdate.class);

    if (errors.hasErrors()) {
      Set<ConstraintViolation<E>> violations = new HashSet<>();

      for(ObjectError o : errors.getAllErrors()) {
        if (o.contains(ConstraintViolation.class)) {
          violations.add((ConstraintViolation<E>) o.unwrap(ConstraintViolation.class));
        }
      }      
      
      throw new ConstraintViolationException(violations);
      }

    return dinaService.validatedUpdate(entity);
  }
}
