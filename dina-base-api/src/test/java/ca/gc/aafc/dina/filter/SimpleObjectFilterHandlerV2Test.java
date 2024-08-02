package ca.gc.aafc.dina.filter;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import org.junit.jupiter.api.Test;

import ca.gc.aafc.dina.dto.DepartmentDto;
import ca.gc.aafc.dina.entity.Department;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
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

  @Test
  public void generateComparator_onSort_expectedOrder() {

    DepartmentDto dep = new DepartmentDto();
    dep.setName("Jim");
    DepartmentDto dep1 = new DepartmentDto();
    dep1.setName("Jim1");
    DepartmentDto dep2 = new DepartmentDto();
    dep2.setName("Jim2");
    DepartmentDto dep3 = new DepartmentDto();
    dep3.setName(null);

    List<DepartmentDto> deps = new ArrayList<>(List.of(dep2, dep1, dep, dep3));
    deps.sort(SimpleObjectFilterHandlerV2.generateComparator("name"));

    assertEquals("Jim", deps.getFirst().getName());
    // null last
    assertNull(deps.getLast().getName());

    // reverse sort
    deps.sort(SimpleObjectFilterHandlerV2.generateComparator("-name"));
    assertEquals("Jim2", deps.getFirst().getName());
    // null last
    assertNull(deps.getLast().getName());
  }
}
