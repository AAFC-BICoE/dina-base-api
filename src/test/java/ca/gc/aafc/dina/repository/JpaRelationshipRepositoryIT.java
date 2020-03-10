package ca.gc.aafc.dina.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.transaction.Transactional;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import ca.gc.aafc.dina.TestConfiguration;
import ca.gc.aafc.dina.dto.DepartmentDto;
import ca.gc.aafc.dina.dto.EmployeeDto;
import ca.gc.aafc.dina.entity.Department;
import ca.gc.aafc.dina.entity.Employee;
import ca.gc.aafc.dina.repository.JpaRelationshipRepository;
import ca.gc.aafc.dina.repository.JpaResourceRepository;
import io.crnk.core.exception.ResourceNotFoundException;
import io.crnk.core.queryspec.IncludeFieldSpec;
import io.crnk.core.queryspec.IncludeRelationSpec;
import io.crnk.core.queryspec.QuerySpec;
import io.crnk.core.resource.list.ResourceList;

@Transactional
@SpringBootTest(classes = TestConfiguration.class)
public class JpaRelationshipRepositoryIT {

  @Inject
  private JpaResourceRepository<EmployeeDto> employeeRepository;
  
  @Inject
  private JpaResourceRepository<DepartmentDto> departmentRepository;

  @Inject
  private JpaRelationshipRepository<DepartmentDto, EmployeeDto> departmentToEmployeeRepository;

  @Inject
  private JpaRelationshipRepository<EmployeeDto, DepartmentDto> employeeToDepartmentRepository;

  @Inject
  private EntityManager entityManager;

  /**
   * Persists a Employee and a Department and associates them.
   */
  protected Employee persistEmployeeWithDepartment(Employee employee, Department department) {
    employee.setDepartment(department);
    entityManager.persist(employee);
    entityManager.persist(department);

    return employee;
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

    entityManager.flush();
    entityManager.refresh(department);

    return department;
  }

  @Test
  public void findOneTargetDepartmentFromSourceEmployee_whenNoFieldsAreSelected_departmentReturnedWithAllFields() {

    Employee employee = persistEmployeeWithDepartment(
      Employee.builder().name("employee").build(),
      Department.builder().name("department").location("Ottawa").build()
    );
    QuerySpec querySpec = new QuerySpec(DepartmentDto.class);

    DepartmentDto dept = employeeToDepartmentRepository.findOneTarget(employee.getId(), "department", querySpec);

    assertNotNull(dept.getUuid());
    assertNotNull(dept.getName());
    assertNotNull(dept.getLocation());
  }

  @Test
  public void findOneTargetDepartmentFromSourceEmployee_whenFieldsAreSelected_departmentReturnedWithSelectedFields() {

    Employee emp = persistEmployeeWithDepartment(
      Employee.builder().name("employee").build(),
      Department.builder().name("department").location("Ottawa").build()
    );
    
    QuerySpec targetQuerySpec = new QuerySpec(DepartmentDto.class);
    targetQuerySpec.setIncludedFields(includeFieldSpecs("name"));
    
    DepartmentDto dept = employeeToDepartmentRepository.findOneTarget(emp.getId(), "department", targetQuerySpec);
    
    assertNotNull(dept.getUuid());
    assertNull(dept.getLocation());
  }
  
  @Test
  public void findOneTargetDepartmentFromSourceEmployee_whenEmployeeExistsAndDepartmentDoesNotExist_throwResourceNotFoundException() {
    Employee emp = Employee.builder().name("employee").build();
    entityManager.persist(emp);
    QuerySpec targetQuerySpec = new QuerySpec(DepartmentDto.class);
    assertThrows(ResourceNotFoundException.class,
        () -> employeeToDepartmentRepository.findOneTarget(emp.getId(), "department", targetQuerySpec));
  }
  
  @Test
  public void findManyTargetEmployeesFromDepartment_whenNoParamsAreSet_allEmployeesAreReturned() {
    Department dept = persistTestDepartmentWith22Employees("test dept");
    
    // Add a second department to ensure that the repository does not fetch emps from a different
    // department.
    persistTestDepartmentWith22Employees("unrelated dept");
    
    QuerySpec querySpec = new QuerySpec(EmployeeDto.class);
    
    ResourceList<EmployeeDto> empDtos = departmentToEmployeeRepository
        .findManyTargets(dept.getUuid(), "employees", querySpec);
    
    assertEquals(22, empDtos.size());
    assertEquals(
        empDtos.stream()
            .map(EmployeeDto::getId)
            .sorted()
            .collect(Collectors.toList()),
        dept.getEmployees().stream()
            .map(Employee::getId)
            .sorted()
            .collect(Collectors.toList())
    );
    
  }
  
  @Test
  public void setRelation_whenDtoEmployeeDepartmentIsChanged_employeeEntityRelatedDepartmentIsChanged() {

    Employee testEmp = persistEmployeeWithDepartment(
      Employee.builder().name("employee").build(),
      Department.builder().name("department").location("Ottawa").build()
    );

    Department newDept = Department.builder().name("new dept").location("Ottawa").build();
    entityManager.persist(newDept);

    EmployeeDto testEmpDto = employeeRepository.findOne(testEmp.getId(),
        new QuerySpec(EmployeeDto.class));

    employeeToDepartmentRepository.setRelation(testEmpDto, newDept.getUuid(), "department");

    assertEquals(newDept.getUuid(), testEmp.getDepartment().getUuid());
  }
  
  @Test
  public void setRelation_whenEmployeeIsMovedToDifferentDept_employeeEntiityRelatedDeptIsChanged() {
    // Create 2 depts. We will move an employee from dept1 to dept2.
    Department dept1 = persistTestDepartmentWith22Employees("dept1");
    Department dept2 = persistTestDepartmentWith22Employees("dept2");
    
    Employee empEntityToMove = dept1.getEmployees().get(0);
    
    EmployeeDto empDtoToMove = employeeRepository.findOne(
        empEntityToMove.getId(),
        new QuerySpec(EmployeeDto.class)
    );

    // Move the employee.
    employeeToDepartmentRepository.setRelation(empDtoToMove, dept2.getUuid(), "department");
    
    assertEquals(dept2, empEntityToMove.getDepartment());
  }
  
  @Test
  public void setRelationEmployeeToDepartment_whenEmployeeAlreadyLinkedToDepartment_relationDoesNotChange() {
    Department dept = persistTestDepartmentWith22Employees("dept");
    Employee emp = dept.getEmployees().get(0);
    EmployeeDto empDto = employeeRepository.findOne(
        emp.getId(),
        new QuerySpec(EmployeeDto.class)
    );
    
    // Do the redundant setRelation.
    employeeToDepartmentRepository.setRelation(empDto, dept.getUuid(), "department");
    
    // Check that the emp is still linked to the same dept.
    assertEquals(dept, emp.getDepartment());
  }
  
  @Test
  public void setRelations_whenEmployeesAreMovedToDifferentDepartment_entityRelationsAreSuccessfullyChanged() {
    // Create a test dept with 22 employees.
    Department dept1 = persistTestDepartmentWith22Employees("dept1");
    
    // Create a dept with no employees.
    Department dept2 = Department.builder()
      .name("dept2")
      .location("Ottawa")
      .build();
    entityManager.persist(dept2);
    
    // Dept 1 should have 22 emps and dept2 should have no emps.
    assertEquals(22, dept1.getEmployees().size());
    assertEquals(0, dept2.getEmployees().size());
    
    // Get the dept2 DTO.
    DepartmentDto dept2Dto = departmentRepository.findOne(dept2.getUuid(), new QuerySpec(DepartmentDto.class));
    
    // Move dept1's emps to dept2
    departmentToEmployeeRepository.setRelations(
        dept2Dto,
        dept1.getEmployees().stream().map(Employee::getId).collect(Collectors.toList()),
        "employees"
    );
    
    // The 22 emps should have been moved from dept1 to dept2.
    assertEquals(22, dept2.getEmployees().size());
    for (Employee emp : dept2.getEmployees()) {
      assertEquals(dept2, emp.getDepartment());
    }
  }
  
  @Test
  public void addRelations_whenEmployeesAreMovedToDifferentDept_entityRelationsAreSuccessfullyChanged() {
    // Create a test dept with 22 emps.
    Department dept1 = persistTestDepartmentWith22Employees("dept1");
    
    // Create a dept with no emps.
    Department dept2 = Department.builder()
      .name("dept2")
      .location("Ottawa")
      .build();
    entityManager.persist(dept2);
    
    // Dept 1 should have 22 emps and dept2 should have no emps.
    assertEquals(22, dept1.getEmployees().size());
    assertEquals(0, dept2.getEmployees().size());
    
    // Get the dept DTO.
    DepartmentDto dept2Dto = departmentRepository.findOne(dept2.getUuid(), new QuerySpec(DepartmentDto.class));
    
    // Move 3 emps from dept1 to dept2.
    List<Serializable> empIds = Arrays.asList(
        dept1.getEmployees().get(0).getId(),
        dept1.getEmployees().get(1).getId(),
        dept1.getEmployees().get(2).getId()
    );
    departmentToEmployeeRepository.addRelations(
        dept2Dto,
        empIds,
        "employees"
    );
    
    // The 3 emps should have been moved from dept1 to dept2.
    assertEquals(
        empIds,
        dept2.getEmployees().stream().map(Employee::getId).collect(Collectors.toList())
    );
    for (Employee emp : dept2.getEmployees()) {
      assertEquals(dept2, emp.getDepartment());
    }
  }
  
  @Test
  public void removeRelations_whenDtoEmployeeDepartmentRelationIsRemoved_entityRelationIsRemoved() {
    // Create a test emp with a linked dept.
    Employee testEmp = persistEmployeeWithDepartment(
      Employee.builder().name("employee").build(),
      Department.builder().name("department").location("Ottawa").build()
    );
    
    // The emp should be linked to a dept.
    assertNotNull(testEmp.getDepartment());
    
    // Get the test emp's DTO.
    EmployeeDto empDto = employeeRepository.findOne(testEmp.getId(), new QuerySpec(EmployeeDto.class));
    
    // Remove the emp's link to the dept.
    employeeToDepartmentRepository.removeRelations(
        empDto,
        Arrays.asList(testEmp.getDepartment().getUuid()),
        "department"
    );
    
    // The emp should not be linked to the dept anymore.
    assertNull(testEmp.getDepartment());
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
