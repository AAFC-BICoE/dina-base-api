package ca.gc.aafc.dina.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Set;

import org.junit.jupiter.api.Test;

import ca.gc.aafc.dina.dto.RelatedEntity;

/**
 * Tests related to {@link ClassAnnotationHelper}.
 *
 */
public class ClassAnnotationHelperTest {

  @Test
  public void findAnnotatedClasses_onClassWithAnnotation_findTheClass() {
    Set<Class<?>> annotatedClasses = ClassAnnotationHelper.findAnnotatedClasses(this.getClass(),
        RelatedEntity.class);
    assertEquals(1, annotatedClasses.size());
    assertEquals(ClassB.class, annotatedClasses.iterator().next());
  }

  static class ClassA {
  }

  @RelatedEntity(ClassA.class)
  static class ClassB {
  }

}
