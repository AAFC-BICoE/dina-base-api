package ca.gc.aafc.dina.util;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

import org.apache.commons.beanutils.PropertyUtils;

/**
 * Mostly a utility class to simplify usage of reflection and {@link org.apache.commons.beanutils.BeanUtils}
 */
public final class ReflectionUtils {

  private ReflectionUtils() {
    // utility class
  }

  /**
   * Creates a new instance of the provided class by using the default constructor.
   *
   * @param clazz
   * @return instance of clazz
   */
  public static <T> T newInstance(Class<T> clazz) {
    try {
      return clazz.getConstructor().newInstance();
    } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
             NoSuchMethodException e) {
      throw new RuntimeException(e);
    }
  }

  public static Method getSetterMethod(String propertyName, Class<?> beanClass) {
    try {
      return new PropertyDescriptor(propertyName, beanClass).getWriteMethod();
    } catch (IntrospectionException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Sets all provided attributes on the target object.
   * As opposed to BeanUtils, this method will throw an exception if the attribute doesn't exist
   * on the target.
   * @param target
   * @param attributes
   * @throws IllegalArgumentException if attributes is not found or there is a type mismatch
   */
  public static <T> void setAttributes(T target, Map<String, Object> attributes) throws IllegalArgumentException {
    for (Map.Entry<String, Object> attribute : attributes.entrySet()) {
      try {
        PropertyUtils.setProperty(target, attribute.getKey(), attribute.getValue());
      } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
        throw new RuntimeException(e);
      }
    }
  }
}
