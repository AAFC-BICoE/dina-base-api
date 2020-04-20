package ca.gc.aafc.dina.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Test;

import ca.gc.aafc.dina.dto.DepartmentDto;
import ca.gc.aafc.dina.entity.Department;
import ca.gc.aafc.dina.mapper.MapperHelper;

/**
 * Test suite to validate methods from the {@link MapperHelper} class
 */
public class MapperHelperTest {

  @Test
  public void getDtoToEntityMapping_ValidBaseClass_DtoMappedToEntity() {
    Map<Class<?>, Class<?>> result = MapperHelper.getDtoToEntityMapping(DepartmentDto.class);

    assertTrue(result.containsKey(DepartmentDto.class));
    assertEquals(Department.class, result.get(DepartmentDto.class));
  }

  @Test
  public void getDtoToEntityMapping_NullInput_ThrowsNullPointer() {
    assertThrows(NullPointerException.class, () -> MapperHelper.getDtoToEntityMapping(null));
  }
}
