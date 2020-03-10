package ca.gc.aafc.dina.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.transaction.Transactional;

import com.google.common.collect.Comparators;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import ca.gc.aafc.dina.TestConfiguration;
import ca.gc.aafc.dina.dto.DepartmentDto;
import ca.gc.aafc.dina.dto.EmployeeDto;
import ca.gc.aafc.dina.entity.Department;
import ca.gc.aafc.dina.entity.Employee;
import ca.gc.aafc.dina.jpa.BaseDAO;
import io.crnk.core.exception.ResourceNotFoundException;
import io.crnk.core.queryspec.Direction;
import io.crnk.core.queryspec.IncludeFieldSpec;
import io.crnk.core.queryspec.IncludeRelationSpec;
import io.crnk.core.queryspec.QuerySpec;
import io.crnk.core.queryspec.SortSpec;
import io.crnk.core.resource.list.ResourceList;

@Transactional
@SpringBootTest(classes = TestConfiguration.class)
public class JpaResourceRepositoryIT {
  
  @Inject
  private JpaResourceRepository<EmployeeDto> employeeRepository;
  
  @Inject
  private JpaResourceRepository<DepartmentDto> departmentRepository;

  @Inject
  private EntityManager entityManager;

  @Inject
  private BaseDAO baseDAO;
  
  // using factory methods from dbi to create a employee and dept and persist them in the repository
  // together
  protected Employee persistEmployeeWithDepartment(Employee employee, Department department) {
    employee.setDepartment(department);
    entityManager.persist(employee);
    entityManager.persist(department);

    return employee;
  }

  protected Employee createPersistedEmployeeWithDepartment() {
    Employee emp = persistEmployeeWithDepartment(
      Employee.builder().name("employee").build(),
      Department.builder().name("department").location("Ottawa").build()
    );

    return emp;
  }

  protected Department persistTestDepartmentWith22Employees(String deptName) {
    Department department = Department.builder().name(deptName).location("Ottawa").build();
    entityManager.persist(department);
    for (int i = 1; i <= 22; i++) {
      Employee emp = Employee.builder()
        .name(String.format("%s employee %s", deptName, i))
        .department(department)
        .build();
      entityManager.persist(emp);
    }

    return department;
  }

  @Test
  public void findOneEmployee_whenNoFieldsAreSelected_employeeReturnedWithAllFields() {
    Employee emp = createPersistedEmployeeWithDepartment();

    EmployeeDto empDto = employeeRepository.findOne(
        emp.getId(),
        new QuerySpec(EmployeeDto.class)
    );

    // Returned emp DTO must have correct values: all fields are present because no selected
    // fields were specified in the QuerySpec
    assertNotNull(empDto);
    assertEquals(emp.getId(), empDto.getId());
    assertEquals(emp.getName(), empDto.getName());
    assertEquals(StringUtils.upperCase(emp.getName()), empDto.getNameUppercase());
    
    // The emp ID should be returned, but not the rest of the emp's attributes.
    assertNotNull(empDto.getDepartment().getUuid());
    assertNull(empDto.getDepartment().getName());
  }

  @Test
  public void findOneDepartment_whenFieldsAreSelected_departmentReturnedWithSelectedFieldsOnly() {
    Department dept = Department.builder()
      .name("test department")
      .location("Ottawa")
      .build();
    entityManager.persist(dept);

    QuerySpec querySpec = new QuerySpec(DepartmentDto.class);
    querySpec.setIncludedFields(includeFieldSpecs("name"));

    DepartmentDto departmentDto = departmentRepository.findOne(dept.getUuid(), querySpec);

    // Returned dept DTO must have correct values: selected fields are present, non-selected
    // fields are null.
    assertNotNull(departmentDto);
    assertEquals(dept.getUuid(), departmentDto.getUuid());
    assertEquals("test department", departmentDto.getName());
    assertNull(departmentDto.getLocation());
    assertNull(departmentDto.getEmployeeCount());
  }
  
  @Test
  public void findOneEmployee_whenDeptIsIncludedAndFieldsAreSelected_employeeWithDeptReturnedWithSelectedFieldsOnly() {
    Employee emp = createPersistedEmployeeWithDepartment();
    
    QuerySpec querySpec = new QuerySpec(EmployeeDto.class);
    querySpec.setIncludedFields(includeFieldSpecs("name"));
    
    QuerySpec nestedDeptSpec = new QuerySpec(DepartmentDto.class);
    nestedDeptSpec.setIncludedFields(includeFieldSpecs("name"));
    
    querySpec.setIncludedRelations(includeRelationSpecs("department"));
    querySpec.setNestedSpecs(Arrays.asList(nestedDeptSpec));
    
    EmployeeDto empDto = employeeRepository.findOne(emp.getId(), querySpec);
    
    assertNotNull(empDto.getName());
    assertNull(empDto.getJob());
    
    assertNotNull(empDto.getDepartment());
    assertNotNull(empDto.getDepartment().getUuid());
    assertNotNull(empDto.getDepartment().getName());
    assertNull(empDto.getDepartment().getLocation());
  }
  
  @Test
  public void findOneEmployee_whenDeptIsIncludedAndNoFieldsAreSelected_employeeAndDeptReturnedWithAllFields() {
    Employee emp = createPersistedEmployeeWithDepartment();
    
    QuerySpec querySpec = new QuerySpec(EmployeeDto.class);
    querySpec.setIncludedRelations(includeRelationSpecs("department"));
    
    EmployeeDto empDto = employeeRepository.findOne(emp.getId(), querySpec);
    
    assertNotNull(empDto.getName());

    assertNotNull(empDto.getDepartment());
    assertNotNull(empDto.getDepartment().getName());
    assertNotNull(empDto.getDepartment().getLocation());
  }
  
  @Test
  public void findOneEmployee_whenDeptIsIncludedButDoesNotExist_employeeReturnedWithNullDept() {
    Employee emp = Employee.builder().name("test employee").build();
    entityManager.persist(emp);
    
    QuerySpec querySpec = new QuerySpec(EmployeeDto.class);
    querySpec.setIncludedRelations(includeRelationSpecs("department"));
    
    EmployeeDto empDto = employeeRepository.findOne(emp.getId(), querySpec);
    
    assertNotNull(empDto.getId());
    assertNotNull(empDto.getName());
    assertNull(empDto.getDepartment());
  }
  
  @Test
  public void findOneEmployee_onEmployeeNotFound_throwsResourceNotFoundException() {
    assertThrows(ResourceNotFoundException.class, () -> employeeRepository.findOne(1, new QuerySpec(EmployeeDto.class)));
  }
  
  @Test
  public void findAll_whenNoSortSpecified_resultsAreUniqueAndSortedByAscendingId() {
    for (int i = 1; i <= 10; i++) {
      Employee emp = Employee.builder().name("emp " + i).job("Developer").build();
      entityManager.persist(emp);
    }
    
    QuerySpec querySpec = new QuerySpec(EmployeeDto.class);
    querySpec.setLimit(Long.valueOf(10));
    ResourceList<EmployeeDto> empDtos = employeeRepository.findAll(querySpec);
    
    // Check that the IDs are in ascending sequence
    Integer idIterator = empDtos.get(0).getId();
    for (EmployeeDto empDto : empDtos) {
      assertEquals(idIterator++, empDto.getId());
    }
  }
  
  @Test
  public void findAll_whenSortingByName_resultsAreSorted() {
    Employee emp1 = Employee.builder().name("A").build();
    entityManager.persist(emp1);
    
    Employee emp2 = Employee.builder().name("B").build();
    entityManager.persist(emp2);
    
    Employee emp3 = Employee.builder().name("C").build();
    entityManager.persist(emp3);
    
    QuerySpec querySpecAscending = new QuerySpec(EmployeeDto.class);
    querySpecAscending.setSort(Arrays.asList(
        new SortSpec(Arrays.asList("name"), Direction.ASC)
    ));
    ResourceList<EmployeeDto> empsWithAscendingNames = employeeRepository
        .findAll(querySpecAscending);
    assertTrue(
        Comparators.isInOrder(
            empsWithAscendingNames.stream()
                .map(EmployeeDto::getName)
                .collect(Collectors.toList()),
            String::compareTo
        ),
        "Names must be sorted alphabetically (ascending)"
    );
    
    QuerySpec querySpecDescending = new QuerySpec(EmployeeDto.class);
    querySpecDescending.setSort(Arrays.asList(
        new SortSpec(Arrays.asList("name"), Direction.DESC)
    ));
    ResourceList<EmployeeDto> empsWithDescendingNames = employeeRepository
        .findAll(querySpecDescending);
    assertTrue(
        Comparators.isInOrder(
            empsWithDescendingNames.stream()
                .map(EmployeeDto::getName)
                .collect(Collectors.toList()),
            (a, b) -> b.compareTo(a)
        ),
        "Names must be sorted alphabetically (descending)"
    );
  }
  
  @Test
  public void findAll_whenPageLimitIsSet_pageSizeIsLimited() {
    final long pageLimit = 9;
    
    for (int i = 1; i <= 100; i++) {
      Employee emp = 
        Employee.builder()
          .name("test employee" + i)
          .build();
      entityManager.persist(emp);
    }
    
    QuerySpec querySpec = new QuerySpec(EmployeeDto.class);
    querySpec.setLimit(pageLimit);
    ResourceList<EmployeeDto> limitedEmps = employeeRepository.findAll(querySpec);
    assertEquals(pageLimit, limitedEmps.size());
  }
  
  @Test
  public void findAll_whenPageOffsetIsSet_pageStartsAfterOffset() {
    List<Employee> newEmps = new ArrayList<>();
    
    for (int i = 1; i <= 100; i++) {
      Employee emp = 
        Employee.builder()
          .name("test employee" + i)
          .build();
      newEmps.add(emp);
      entityManager.persist(emp);
    }
    
    final int offset = 15;
    final Integer expectedEmpId = newEmps.get(offset).getId();
    assertNotNull(expectedEmpId);

    
    QuerySpec querySpec = new QuerySpec(EmployeeDto.class);
    querySpec.setOffset(offset);
    List<EmployeeDto> employeeDtos = employeeRepository.findAll(querySpec);
    assertEquals(expectedEmpId, employeeDtos.get(0).getId());
  }
  
  @Test
  public void findAll_whenIdsArgumentIsSet_resultsAreFilteredById() {
    List<Employee> newEmps = new ArrayList<>();
    
    for (int i = 1; i <= 10; i++) {
      Employee emp = 
        Employee.builder()
          .name("test employee" + i)
          .build();
      newEmps.add(emp);
      entityManager.persist(emp);
    }
    
    Collection<Serializable> expectedIds = Arrays.asList(
        newEmps.get(2).getId(),
        newEmps.get(4).getId(),
        newEmps.get(6).getId()
    );
    
    QuerySpec querySpec = new QuerySpec(EmployeeDto.class);
    List<EmployeeDto> empDtos = employeeRepository.findAll(expectedIds, querySpec);
    
    assertEquals(
        expectedIds,
        empDtos.stream().map(EmployeeDto::getId).collect(Collectors.toList())
    );
  }
  
  @Test
  public void createEmployee_onSuccess_returnEmployeeWithId() {
    EmployeeDto newEmp = new EmployeeDto();
    newEmp.setName("test employee");
    
    EmployeeDto createdEmp = employeeRepository.create(newEmp);
    
    assertNotNull(createdEmp.getId());
    assertEquals("test employee", createdEmp.getName());
    
    Employee empEntity = entityManager.find(Employee.class, createdEmp.getId());
    assertNotNull(empEntity.getId());
    assertEquals("test employee", empEntity.getName());
  }
  
  @Test
  public void createDept_whenAllEmpsOfADifferentDeptAreLinked_allEmpEntitiesAreReLinked() {
    Department dept1Entity = persistTestDepartmentWith22Employees("dept 1");
    
    List<Employee> emps = dept1Entity.getEmployees();
    
    // Set up a DepartmentDto that will take all of the emps from dept1.
    DepartmentDto dept2Dto = new DepartmentDto();
    dept2Dto.setName("dept2");
    dept2Dto.setLocation("Ottawa");
    dept2Dto.setEmployees(
        // Set the emps as EmployeeDtos holding only the ID attribute required for linking.
        emps.stream()
            .map(Employee::getId)
            .map(empId -> {
              EmployeeDto empDto = new EmployeeDto();
              empDto.setId(empId);
              return empDto;
            })
            .collect(Collectors.toList())
    );
    
    DepartmentDto savedDept2Dto = departmentRepository.create(dept2Dto);
    
    Department dept2Entity = baseDAO.findOneById(savedDept2Dto.getUuid(), Department.class);
    
    // Check that the emps were moved to dept2.
    emps.forEach(emp -> assertEquals(dept2Entity, emp.getDepartment()));
  }
  
  @Test
  public void saveEmployee_onSuccess_employeeEntityIsModified() {
    // Create the test employee.
    Employee testEmp = Employee.builder().name("test employee").build();
    entityManager.persist(testEmp);
    
    // Get the test employee's DTO.
    QuerySpec querySpec = new QuerySpec(EmployeeDto.class);
    EmployeeDto testEmpDto = employeeRepository.findOne(testEmp.getId(), querySpec);
    
    // Change the DTO's job value.
    testEmpDto.setJob("edited job");
    
    // Save the DTO using the repository.
    employeeRepository.save(testEmpDto);
    
    // Check that the employee entity has the new seq value.
    assertEquals("edited job", testEmp.getJob());
  }
  
  @Test
  public void saveEmployeeWithNewDept_onSuccess_employeeEntityIsModified() {
    // Create the test employee.
    Employee testEmp = Employee.builder().name("test employee").build();
    entityManager.persist(testEmp);
    
    Department testDept = Department.builder().name("test dept").location("Ottawa").build();
    entityManager.persist(testDept);
    
    // Get the test employee's DTO.
    QuerySpec querySpec = new QuerySpec(EmployeeDto.class);
    EmployeeDto testEmpDto = employeeRepository.findOne(testEmp.getId(), querySpec);
    
    // Change the test employee's dept to the new department.
    DepartmentDto newDeptDto = new DepartmentDto();
    newDeptDto.setUuid(testDept.getUuid());
    testEmpDto.setDepartment(newDeptDto);
    
    // Save the DTO using the repository.
    EmployeeDto updatedEmpDto = employeeRepository.save(testEmpDto);
    
    // Check that the updated employee has the new dept id.
    assertNotNull(testEmp.getDepartment().getUuid());
    assertEquals(testDept.getUuid(), updatedEmpDto.getDepartment().getUuid());
    assertEquals(testDept.getUuid(), testEmp.getDepartment().getUuid());
  }
  
  @Test
  public void saveExistingEmployeeAndRemoveLinkedDept_onSuccess_employeeEntityIsModified() {
    Employee testEmp = createPersistedEmployeeWithDepartment();
    
    assertNotNull(testEmp.getDepartment().getUuid());
    
    // Get the test emp's DTO.
    QuerySpec querySpec = new QuerySpec(EmployeeDto.class);
    EmployeeDto testEmpDto = employeeRepository.findOne(testEmp.getId(), querySpec);
    
    // The emp's dept id should not be null.
    assertNotNull(testEmpDto.getDepartment().getUuid());
    
    testEmpDto.setDepartment(null);

    // Save the DTO using the repository.
    EmployeeDto updatedEmpDto = employeeRepository.save(testEmpDto);
    
    // Check that the dept is null in the dto and the entity.
    assertNull(updatedEmpDto.getDepartment());
    assertNull(testEmp.getDepartment());
  }
  
  @Test
  public void deleteEmployee_onEmployeeLookup_employeeNotFound() {
    Employee emp = Employee.builder().name("test employee").build();
    entityManager.persist(emp);
    employeeRepository.delete(emp.getId());
    assertNull(entityManager.find(Employee.class, emp.getId()));
  }

  @Test
  public void deleteEmployee_onEmployeeNotFound_throwResourceNotFoundException() {
    assertThrows(ResourceNotFoundException.class, () -> employeeRepository.delete(1));
  }

  /**
   * Get a List<IncludeFieldSpec> from of an array of field names.
   * E.g. includeFieldSpecs("name", "description")
   * 
   * @param includedFields strings
   * @return List<IncludeFieldSpec>
   */
  protected static List<IncludeFieldSpec> includeFieldSpecs(String... includedFields) {
    return Arrays.asList(includedFields)
        .stream()
        .map(Arrays::asList)
        .map(IncludeFieldSpec::new)
        .collect(Collectors.toList());
  }
  
  /**
   * Get a List<IncludeRelationSpec> from an array of relation names.
   * E.g. includeRelationSpecs("department")
   * 
   * @param includedRelations strings
   * @return List<IncludeRelationSpec>
   */
  protected static List<IncludeRelationSpec> includeRelationSpecs(String... includedRelations) {
    return Arrays.asList(includedRelations)
        .stream()
        .map(Arrays::asList)
        .map(IncludeRelationSpec::new)
        .collect(Collectors.toList());
  }

}
