package ca.gc.aafc.dina.mapper;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang3.reflect.MethodUtils;

import lombok.NonNull;
import lombok.SneakyThrows;

/**
 * <p>
 * Handles Custom field resolvers for mapping values between DTOs and Entities.
 * Custom field resolvers are assumed to be declared inside the DTO class.
 * <p>
 * <p>
 * Field resolvers must have at least one parameter of type entity or Dto. A
 * Field Resolver which accepts a parameter of Entity type would map to the
 * associated field in the DTO and vise versa.
 * <p>
 * 
 * <p>
 * Field resolver return types must also match the mapping target field type.
 * <p>
 *
 * @param <D>
 *              - Dto type
 * @param <E>
 *              - Entity Type
 */
public class CustomFieldHandler<D, E> {

  private final Class<D> dtoClass;
  private final Class<E> entityClass;

  private final Object resolverHolder;

  private final Map<String, Method> dtoResolvers = new HashMap<>();
  private final Map<String, Method> entityResolvers = new HashMap<>();

  public CustomFieldHandler(Class<D> dtoClass, Class<E> entityClass) {
    this(
      dtoClass,
      entityClass,
      MethodUtils.getMethodsListWithAnnotation(dtoClass, CustomFieldResolver.class));
  }

  public CustomFieldHandler(
    @NonNull Class<D> dtoClass,
    @NonNull Class<E> entityClass,
    @NonNull List<Method> resolvers
  ) {
    this.dtoClass = dtoClass;
    this.entityClass = entityClass;
    this.resolverHolder = getResolverHolderInstancce(dtoClass);
    initResolvers(resolvers);
  }

  @SneakyThrows
  private Object getResolverHolderInstancce(Class<?> clazz) {
    return clazz.newInstance();
  }

  /**
   * <p>
   * Scans the dto class and adds the custom field resolvers to the appropriate
   * resolver maps.
   * <p>
   * 
   * @throws IllegalStateException
   *                                 if the custom field resolver has incorrect
   *                                 parameters or return types.
   */
  private void initResolvers(List<Method> methods) {
    for (Method method : methods) {
      validateResolverParameter(method);
      mapResolverToMap(method);
    }
    validateResolverReturnType(dtoClass, dtoResolvers);
    validateResolverReturnType(entityClass, entityResolvers);
  }

  /**
   * Adds the custom field resolvers to the appropriate resolver maps.
   * 
   * @param resolver
   */
  private void mapResolverToMap(Method resolver) {
    Class<?> methodParamType = resolver.getParameterTypes()[0];
    CustomFieldResolver customFieldResolver = resolver.getAnnotation(CustomFieldResolver.class);

    if (methodParamType.equals(entityClass)) {
      dtoResolvers.put(customFieldResolver.fieldName(), resolver);
    } else if (methodParamType.equals(dtoClass)) {
      entityResolvers.put(customFieldResolver.fieldName(), resolver);
    } else {
      throwInvalidParameterResponse(resolver.getName());
    }
  }

  /**
   * Throws IllegalStateException if Field resolvers does not have one parameter
   * of type entity or Dto
   * 
   * @param resolver
   *                   - resolver to validate
   */
  private void validateResolverParameter(Method resolver) {
    boolean isInvalid = resolver.getParameterCount() != 1 
      || (!resolver.getParameterTypes()[0].equals(entityClass)
      && !resolver.getParameterTypes()[0].equals(dtoClass));

    if (isInvalid) {
      throwInvalidParameterResponse(resolver.getName());
    }
  }

  /**
   * Throws a new IllegalStateException with error message for an inccorect
   * resolver parameter type.
   * 
   * @param methodName
   *                     - method name for message.
   */
  private void throwInvalidParameterResponse(String methodName) {
    throw new IllegalStateException("Custom field resolver " + methodName
        + " should accept one parameter of type entity or dto");
  }

  /**
   * Validates the given resolvers have return types matching the fields for the
   * given class.
   * 
   * @param <T>
   *                    - Class type
   * @param claz
   *                    - Given class to compare
   * @param resolvers
   *                    - resolvers to validate
   * @throws IllegalStateException
   *                                 - if the custom field resolver has return
   *                                 types.
   */
  @SneakyThrows
  private static <T> void validateResolverReturnType(Class<T> claz, Map<String, Method> resolvers) {
    for (Entry<String, Method> entry : resolvers.entrySet()) {
      Class<?> fieldType = claz.getDeclaredField(entry.getKey()).getType();
      Class<?> methodReturnType = entry.getValue().getReturnType();

      if (!fieldType.equals(methodReturnType)) {
        throw new IllegalStateException(
            "Custom field resolver " + entry.getValue().getName() + " expected return type "
                + fieldType.getName() + " but was " + methodReturnType.getName());
      }
    }
  }

  /**
   * Resolve the given selected custom fields from source to target.
   * 
   * @param <T>
   *                         - source Type
   * @param <S>
   *                         - target Type
   * @param selectedFields
   *                         - fields to resolve
   * @param source
   *                         - source bean
   * @param target
   *                         - target bean
   */
  public <T, S> void resolveFields(Set<String> selectedFields, T source, S target) {
    Map<String, Method> selectedResolvers = getSelectedResolvers(source);
    selectedResolvers.entrySet().removeIf(e -> !selectedFields.contains(e.getKey()));
    mapCustomFieldsToTarget(source, target, selectedResolvers);
  }

  /**
   * Returns a copy of the appropriate custom field resolvers for the given
   * source.
   * 
   * @param <T>
   *                 - source object type
   * @param source
   *                 - source object for the mapping
   * @return - a copy of the appropriate custom field resolvers
   */
  private <T> Map<String, Method> getSelectedResolvers(T source) {
    if (dtoClass == source.getClass()) {
      return new HashMap<>(entityResolvers);
    } else if (entityClass == source.getClass()) {
      return new HashMap<>(dtoResolvers);
    } else {
      throw new IllegalArgumentException("Expected source type of " + dtoClass.getSimpleName()
          + " or " + entityClass.getSimpleName() + " but was " + source.getClass());
    }
  }

  /**
   * Maps the custom field resolvers of a given source to a given target.
   *
   * @param <T>
   *                       - Type of target
   * @param <S>
   *                       - Type of source
   * @param source
   *                       - source of the mapping
   * @param target
   *                       - target of the mapping
   * @param resolvers
   *                       - custom resolvers to apply
   */
  @SneakyThrows
  private <T, S> void mapCustomFieldsToTarget(S source, T target, Map<String, Method> resolvers) {
    for (Entry<String, Method> entry : resolvers.entrySet()) {
      Object mappedValue = entry.getValue().invoke(resolverHolder, source);
      PropertyUtils.setProperty(target, entry.getKey(), mappedValue);
    }
  }

  /**
   * Returns true if the given field name has a custom resolver
   *
   * @param fieldName
   *                    - field name of field to check
   * @return - true if the given field has custom resolvers.
   */
  public boolean hasCustomFieldResolver(String fieldName) {
    return dtoResolvers.keySet().contains(fieldName)
        || entityResolvers.keySet().contains(fieldName);
  }

}
