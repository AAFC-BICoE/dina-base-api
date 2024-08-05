package ca.gc.aafc.dina.filter;

import java.util.function.Predicate;

import org.junit.jupiter.api.Test;

import ca.gc.aafc.dina.dto.DepartmentDto;
import ca.gc.aafc.dina.entity.Department;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SimpleObjectFilterHandlerV2Test {

  @Test
  public void getRestriction_onFilter_predicateCreated() {

    String content = "filter[departmentDetails.note][EQ]=best one&filter[name][EQ]=Jim";
    QueryComponent queryComponent = QueryStringParser.parse(content);

    FilterGroup fg = queryComponent.getFilterGroup().get();
    assertEquals(2, fg.getComponents().size());

    Predicate<DepartmentDto> p = SimpleObjectFilterHandlerV2.buildPredicate(null, fg.getComponents());

    DepartmentDto dep = new DepartmentDto();
    dep.setName("Jim");

    Department.DepartmentDetails depDetails = new Department.DepartmentDetails("best one");
    dep.setDepartmentDetails(depDetails);
    assertTrue(p.test(dep));

    dep.setName("Tim");
    assertFalse(p.test(dep));
  }
}
