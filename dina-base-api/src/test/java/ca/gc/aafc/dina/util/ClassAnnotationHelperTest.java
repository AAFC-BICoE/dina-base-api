package ca.gc.aafc.dina.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;

import ca.gc.aafc.dina.entity.DinaEntity;
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

  static class ClassA implements DinaEntity {
    @Override
    public Integer getId() {
      return null;
    }

    @Override
    public UUID getUuid() {
      return null;
    }

    @Override
    public void setUuid(UUID uuid) {

    }

    @Override
    public String getCreatedBy() {
      return null;
    }

    @Override
    public OffsetDateTime getCreatedOn() {
      return null;
    }
  }

  @RelatedEntity(ClassA.class)
  static class ClassB {
  }

}
