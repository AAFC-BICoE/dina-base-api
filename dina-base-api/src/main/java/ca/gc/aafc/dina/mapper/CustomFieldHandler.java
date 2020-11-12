package ca.gc.aafc.dina.mapper;

import lombok.NonNull;
import lombok.SneakyThrows;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang3.reflect.FieldUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 * Handles Custom field resolvers for mapping values between DTOs and Entities. Resolvers are
 * declared on the class which needs the field resolved.
 * <p>
 * <p>
 * A field resolver should always have a parameter type that matches the source class of the
 * mapping.
 * <p>
 *
 * <p>
 * Field resolver return types must also match the mapping target field type.
 * <p>
 *
 * @param <D> - Dto type
 * @param <E> - Entity Type
 */
public class CustomFieldHandler<D, E> {

  private final Class<D> dtoClass;
  private final Class<E> entityClass;

  private final Map<String, Method> dtoResolvers;
  private final Map<String, Method> entityResolvers;

  public CustomFieldHandler(
    @NonNull Class<D> dtoClass,
    @NonNull Class<E> entityClass
  ) {
    this.dtoClass = dtoClass;
    this.entityClass = entityClass;
    this.dtoResolvers = initResolvers(dtoClass, entityClass);
    this.entityResolvers = initResolvers(entityClass, dtoClass);
  }

  private Map<String, Method> initResolvers(Class<?> targetClass, Class<?> source) {
    return Arrays
      .stream(FieldUtils.getFieldsWithAnnotation(targetClass, CustomFieldResolver.class))
      .collect(Collectors.toMap(Field::getName, f -> parseCustomFieldResolver(f, source)));
  }

  private static Method parseCustomFieldResolver(Field field, Class<?> paramType) {
    CustomFieldResolver cfr = field.getAnnotation(CustomFieldResolver.class);
    if (cfr == null) {
      throw new IllegalArgumentException("The given field does not contain a custom field resolver");
    }

    try {
      Method method = field.getDeclaringClass().getDeclaredMethod(cfr.setterMethod(), paramType);
      validateResolverReturnType(cfr.setterMethod(), field.getType(), method.getReturnType());
      return method;
    } catch (NoSuchMethodException e) {
      throw new IllegalArgumentException(
        "Expected a method with name: " + cfr.setterMethod()
        + " paramType: " + paramType.getSimpleName()
        + " return type: " + field.getType()
        + " but none was found");
    }
  }

  private static void validateResolverReturnType(String name, Class<?> expected, Class<?> actual) {
    if (actual != expected) {
      throw new IllegalArgumentException(
        "Custom field resolver " + name + " should return a type of: " + expected.getSimpleName());
    }
  }

  /**
   * Resolve the given selected custom fields from source to target.
   *
   * @param <T>            - source Type
   * @param <S>            - target Type
   * @param selectedFields - fields to resolve
   * @param source         - source bean
   * @param target         - target bean
   */
  public <T, S> void resolveFields(Set<String> selectedFields, T source, S target) {
    validateClassIsSupported(target.getClass());
    if (entityClass == target.getClass()) {
      resolveFields(source, target, entityResolvers, selectedFields);
    } else if (dtoClass == target.getClass()) {
      resolveFields(source, target, dtoResolvers, selectedFields);
    }
  }

  @SneakyThrows
  private <T, S> void resolveFields(
    T source,
    S target,
    Map<String, Method> resolvers,
    Set<String> selectedFields
  ) {
    for (Map.Entry<String, Method> entry : resolvers.entrySet()) {
      String fieldName = entry.getKey();
      Method method = entry.getValue();
      if (selectedFields.contains(fieldName)) {
        Object mappedValue = method.invoke(target, source);
        PropertyUtils.setProperty(target, fieldName, mappedValue);
      }
    }
  }

  /**
   * Validates a given class is supported by this handler or an IllegalArgumentException is thrown.
   *
   * @param clazz - class to validate
   */
  private void validateClassIsSupported(Class<?> clazz) {
    if (clazz != dtoClass && clazz != entityClass) {
      throw new IllegalArgumentException(
        "Expected clazz type of " + dtoClass.getSimpleName() + " or "
        + entityClass.getSimpleName() + " but was " + clazz.getSimpleName());
    }
  }

  /**
   * Returns true if the given field name has a custom resolver
   *
   * @param fieldName - field name of field to check
   * @return - true if the given field has custom resolvers.
   */
  public boolean hasCustomFieldResolver(String fieldName) {
    return dtoResolvers.containsKey(fieldName) || entityResolvers.containsKey(fieldName);
  }

}
