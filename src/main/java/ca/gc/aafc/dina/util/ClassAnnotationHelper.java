package ca.gc.aafc.dina.util;

import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.Set;

import lombok.NonNull;
import lombok.extern.log4j.Log4j2;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;


/**
 * Helper class to find annotated classes.
 */
@Log4j2
public class ClassAnnotationHelper {

  /**
   * Utility class
   */
  private ClassAnnotationHelper() {
  }

  /**
   * Find all classes from package represented by the basePackage class that are annotated with the
   * annotationClass.
   * 
   * @param basePackage class belonging to the package to scan
   * @param annotationClass class representing the annotation class
   * @return a {@link Set} with all the classes or an empty set.
   */
  public static Set<Class<?>> findAnnotatedClasses(@NonNull Class<?> basePackage,
      @NonNull Class<? extends Annotation> annotationClass) {
    ClassPathScanningCandidateComponentProvider provider = newComponentScannerByAnnotation(
        annotationClass);
    Set<Class<?>> classList = new HashSet<>();
    try {
      for (BeanDefinition beanDef : provider
          .findCandidateComponents(basePackage.getPackage().getName())) {
        classList.add(Class.forName(beanDef.getBeanClassName()));
      }
    } catch (ClassNotFoundException e) {
      // Not really possible since the classes are from package scanning
      log.error(e);
    }
    return classList;
  }

  /**
   * Create a scanner filter to get all classes with a specific annotation.
   * 
   * @param annotationClass class representing the annotation class
   * @return
   */
  private static ClassPathScanningCandidateComponentProvider newComponentScannerByAnnotation(
      Class<? extends Annotation> annotationClass) {
    // Don't extract default filters
    ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(
        false);
    provider.addIncludeFilter(new AnnotationTypeFilter(annotationClass));
    return provider;
  }

}
