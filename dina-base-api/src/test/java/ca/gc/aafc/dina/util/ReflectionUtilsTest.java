package ca.gc.aafc.dina.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

import org.junit.jupiter.api.Test;

import ca.gc.aafc.dina.dto.DepartmentDto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class ReflectionUtilsTest {

  @Test
  public void testReflectionUtilsMethods() throws InvocationTargetException, IllegalAccessException {
    DepartmentDto dto = ReflectionUtils.newInstance(DepartmentDto.class);
    assertNotNull(dto);

    Method setter = ReflectionUtils.getSetterMethod("name", DepartmentDto.class);
    setter.invoke(dto, "the name");
    assertEquals("the name", dto.getName());

    ReflectionUtils.setAttributes(dto, Map.of("name", "new name"));
    assertEquals("new name", dto.getName());
  }
}
