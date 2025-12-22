package ca.gc.aafc.dina.filter;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import org.junit.jupiter.api.Test;

import ca.gc.aafc.dina.dto.DepartmentDto;
import ca.gc.aafc.dina.entity.Department;

import ca.gc.aafc.dina.exception.UnknownAttributeException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SimpleObjectFilterHandlerV2Test {

  @Test
  public void getRestriction_onFilter_predicateCreated() {

    String content = "filter[departmentDetails.note][EQ]=best one&filter[name][EQ]=Jim";
    QueryComponent queryComponent = QueryStringParser.parse(content);

    FilterGroup fg = queryComponent.getFilterGroup().get();
    assertEquals(2, fg.getComponents().size());

    Predicate<DepartmentDto> p = SimpleObjectFilterHandlerV2.createPredicate(fg);

    DepartmentDto dep = new DepartmentDto();
    dep.setName("Jim");

    Department.DepartmentDetails depDetails = new Department.DepartmentDetails("best one");
    dep.setDepartmentDetails(depDetails);
    assertTrue(p.test(dep));

    dep.setName("Tim");
    assertFalse(p.test(dep));
  }

  @Test
  public void getRestriction_onInFilter_predicateCreated() {

    String content = "filter[name][IN]=Jim,\"jimmy,jim\"";
    QueryComponent queryComponent = QueryStringParser.parse(content);

    FilterGroup fe = queryComponent.getFilterGroup().get();
    Predicate<DepartmentDto> p = SimpleObjectFilterHandlerV2.createPredicate(fe);

    DepartmentDto dep = new DepartmentDto();
    dep.setName("Jim");

    Department.DepartmentDetails depDetails = new Department.DepartmentDetails("best one");
    dep.setDepartmentDetails(depDetails);
    assertTrue(p.test(dep));

    dep.setName("Tim");
    assertFalse(p.test(dep));

    dep.setName("jimmy,jim");
    assertTrue(p.test(dep));
  }

  @Test
  public void getRestriction_onNonExistingProperty_UnknownAttributeException() {

    String content = "filter[notname][EQ]=Jim";
    QueryComponent queryComponent = QueryStringParser.parse(content);

    FilterComponent fg = queryComponent.getFilters();

    Predicate<DepartmentDto> p = SimpleObjectFilterHandlerV2.createPredicate(fg);
    DepartmentDto dep = new DepartmentDto();
    dep.setName("Jim");

    assertThrows(UnknownAttributeException.class,
      () -> p.test(dep));
  }

  @Test
  public void getRestriction_onFilterWithLike_predicateCreated() {

    String content = "filter[name][LIKE]=Ji%";
    QueryComponent queryComponent = QueryStringParser.parse(content);

    Predicate<DepartmentDto> p = SimpleObjectFilterHandlerV2.createPredicate(queryComponent.getFilters());

    DepartmentDto dep = new DepartmentDto();
    dep.setName("Jim");
    assertTrue(p.test(dep));

    dep.setName("Jam");
    assertFalse(p.test(dep));

    content = "filter[name][LIKE]=%im";
    queryComponent = QueryStringParser.parse(content);

    p = SimpleObjectFilterHandlerV2.createPredicate(queryComponent.getFilters());

    dep.setName("Jim");
    assertTrue(p.test(dep));

    dep.setName("Jam");
    assertFalse(p.test(dep));
  }

  /**
   * ilike version (case-insensitive)
   */
  @Test
  public void getRestriction_onFilterWithiLike_predicateCreated() {

    String content = "filter[name][ILIKE]=Ji%";
    QueryComponent queryComponent = QueryStringParser.parse(content);

    Predicate<DepartmentDto> p = SimpleObjectFilterHandlerV2.createPredicate(queryComponent.getFilters());

    DepartmentDto dep = new DepartmentDto();
    dep.setName("jim");
    assertTrue(p.test(dep));

    dep.setName("jam");
    assertFalse(p.test(dep));

    content = "filter[name][ILIKE]=%iM";
    queryComponent = QueryStringParser.parse(content);

    p = SimpleObjectFilterHandlerV2.createPredicate(queryComponent.getFilters());

    dep.setName("jim");
    assertTrue(p.test(dep));

    dep.setName("jam");
    assertFalse(p.test(dep));
  }

  @Test
  public void getRestriction_onFilterOnNestedList_predicateCreated() {

    String content = "filter[aliases.name][EQ]=alf";
    QueryComponent queryComponent = QueryStringParser.parse(content);

    Predicate<DepartmentDto> p = SimpleObjectFilterHandlerV2.createPredicate(queryComponent.getFilters());

    DepartmentDto dep = new DepartmentDto();
    dep.setName("jim");
    dep.setAliases(List.of(new Department.DepartmentAlias("alf")));

    DepartmentDto dep2 = new DepartmentDto();
    dep2.setName("jimbo");
    dep2.setAliases(List.of(new Department.DepartmentAlias("elf")));

    assertTrue(p.test(dep));
    assertFalse(p.test(dep2));
    dep.setName("jam");
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
    // null first (since it is reversed)
    assertNull(deps.getFirst().getName());

    assertEquals("Jim2", deps.get(1).getName());
  }

  @Test
  public void generateComparator_onSortList_expectedOrder() {

    DepartmentDto dep = DepartmentDto.builder()
        .name("Jim").location("b").build();
    DepartmentDto dep1 =  DepartmentDto.builder()
      .name("Jim1").build();
    DepartmentDto dep2 =  DepartmentDto.builder()
      .name("Jim").location("a").build();
    DepartmentDto dep3 =  DepartmentDto.builder()
      .name(null).build();

    List<DepartmentDto> deps = new ArrayList<>(List.of(dep2, dep1, dep, dep3));
    deps.sort(SimpleObjectFilterHandlerV2.generateComparator(List.of("name", "location")));

    assertEquals("Jim", deps.getFirst().getName());
    assertEquals("a", deps.getFirst().getLocation());

    // null last
    assertNull(deps.getLast().getName());
  }

  @Test
  public void generateComparator_onNonExistingProperty_UnknownAttributeException() {
    DepartmentDto dep = DepartmentDto.builder()
      .name("Jim").location("b").build();
    DepartmentDto dep1 =  DepartmentDto.builder()
      .name("Jim1").build();

    List<DepartmentDto> deps = new ArrayList<>(List.of(dep, dep1));
    assertThrows(UnknownAttributeException.class,
      () -> deps.sort(SimpleObjectFilterHandlerV2.generateComparator(List.of("notname"))));
  }
}
