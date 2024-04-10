package ca.gc.aafc.dina.repository;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import ca.gc.aafc.dina.dto.DepartmentDto;
import ca.gc.aafc.dina.entity.Department;
import ca.gc.aafc.dina.service.PredicateBasedReadOnlyDinaService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class ReadOnlyDinaRepositoryV2Test {

  @Test
  public void readOnlyDinaRepository_onFindAll_expectedResultsReturned() {
    List<DepartmentDto> departmentDtoList = new ArrayList<>();

    DepartmentDto dep = new DepartmentDto();
    dep.setUuid(UUID.randomUUID());
    dep.setName("Jim");
    Department.DepartmentDetails depDetails = new Department.DepartmentDetails("best one");
    dep.setDepartmentDetails(depDetails);

    DepartmentDto dep2 = new DepartmentDto();
    dep2.setUuid(UUID.randomUUID());
    dep2.setName("Jim");
    Department.DepartmentDetails depDetails2 = new Department.DepartmentDetails("second best one");
    dep2.setDepartmentDetails(depDetails2);

    DepartmentDto dep3 = new DepartmentDto();
    dep3.setUuid(UUID.randomUUID());
    dep3.setName("Jimmy");
    Department.DepartmentDetails depDetails3 = new Department.DepartmentDetails("a");
    dep3.setDepartmentDetails(depDetails3);

    departmentDtoList.add(dep);
    departmentDtoList.add(dep2);
    departmentDtoList.add(dep3);

    ReadOnlyDinaRepositoryV2<UUID, DepartmentDto> repo = new ReadOnlyDinaRepositoryV2<>(new TestService(departmentDtoList));
    List<DepartmentDto> results = repo.findAll("filter[departmentDetails.note][EQ]=a,best one&filter[name][EQ]=Jim");
    assertEquals(1, results.size());

    assertNotNull(repo.findOne(dep.getUuid()));
  }

  private static class TestService implements PredicateBasedReadOnlyDinaService<UUID, DepartmentDto> {

    private final List<DepartmentDto> list;
    public TestService(List<DepartmentDto> list) {
      this.list = list;
    }

    @Override
    public List<DepartmentDto> findAll(Predicate<DepartmentDto> predicate, Integer pageOffset,
                                       Integer pageLimit) {
      Stream<DepartmentDto> stream = list.stream().filter(predicate);

      if(pageOffset != null) {
        stream = stream.skip(pageOffset);
      }
      if(pageLimit != null) {
        stream = stream.limit(pageLimit);
      }
      return stream.collect(Collectors.toList());
    }

    @Override
    public DepartmentDto findOne(UUID key) {
      return list.stream().filter( d -> key.equals(d.getUuid())).findFirst().orElse(null);
    }
  }
}
