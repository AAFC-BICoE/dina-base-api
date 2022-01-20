package ca.gc.aafc.dina.repository;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.inject.Inject;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import ca.gc.aafc.dina.TestDinaBaseApp;
import ca.gc.aafc.dina.dto.DepartmentDto;
import ca.gc.aafc.dina.dto.PersonDTO;
import ca.gc.aafc.dina.entity.Department;
import ca.gc.aafc.dina.entity.Person;
import ca.gc.aafc.dina.jpa.BaseDAO;
import ca.gc.aafc.dina.repository.meta.DinaMetaInfo;
import ca.gc.aafc.dina.testsupport.DatabaseSupportService;

import io.crnk.core.exception.ResourceNotFoundException;
import io.crnk.core.queryspec.Direction;
import io.crnk.core.queryspec.FilterOperator;
import io.crnk.core.queryspec.IncludeRelationSpec;
import io.crnk.core.queryspec.PathSpec;
import io.crnk.core.queryspec.QuerySpec;
import io.crnk.core.queryspec.SortSpec;
import io.crnk.core.resource.list.ResourceList;
import io.crnk.core.resource.meta.PagedMetaInformation;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = TestDinaBaseApp.class)
public class DinaRepositoryIT {

  @Inject
  private DinaRepository<PersonDTO, Person> personRepository;

  @Inject
  private DinaRepository<DepartmentDto, Department> departmentRepository;

  @Inject
  private BaseDAO baseDAO;

  @Inject
  private DatabaseSupportService databaseSupportService;

  private Department singleRelationUnderTest;
  private List<Department> collectionRelationUnderTest;

  private PersonDTO createTestData(boolean includeDepartmentData) {
    PersonDTO person = PersonDTO.builder()
        .nickNames(Arrays.asList("d", "z", "q").toArray(new String[0]))
        .name(RandomStringUtils.randomAlphabetic(4))
        .group(RandomStringUtils.randomAlphabetic(4))
        .build();

    // Generate department data with person if requested.
    if (includeDepartmentData) {
      List<DepartmentDto> departments = new ArrayList<DepartmentDto>();
      for (int i = 0; i < 10; i++) {
        departments.add(departmentRepository.create(DepartmentDto.builder()
            .name(RandomStringUtils.randomAlphabetic(4))
            .location(RandomStringUtils.randomAlphabetic(4))
            .build()
        ));
      }
      
      // For the single department, just use the first department created from the list.
      person.setDepartment(departments.get(0));
      person.setDepartments(departments);
    }

    return personRepository.create(person);
  }

  private void cleanUpTestData(PersonDTO personToRemove) {
    // Check if person has department data to remove.
    if (personToRemove.getDepartment() != null) {
      List<DepartmentDto> departmentsToRemove = personToRemove.getDepartments();

      // Unlink from the person.
      personToRemove.setDepartment(null);
      personToRemove.setDepartments(null);
      personRepository.save(personToRemove);

      // Delete the multiple departments list. The first one is also shared with the
      // .getDepartment() so it will automatically be removed when going though the
      // list.
      departmentsToRemove.forEach(department -> {
        departmentRepository.delete(department.getUuid());
      });
    }

    // Now remove the person since all department data has been removed.
    personRepository.delete(personToRemove.getUuid());
  }

  private void cleanUpPersonList(List<PersonDTO> personListToRemove) {
    personListToRemove.forEach(person -> cleanUpTestData(person));
  }

  private void assertEqualsPersonDtos(PersonDTO dto, PersonDTO result, boolean testRelations) {
    assertEquals(dto.getUuid(), result.getUuid());
    assertEquals(dto.getName(), result.getName());
    assertArrayEquals(dto.getNickNames(), result.getNickNames());
    if (testRelations) {
      assertEquals(dto.getDepartment().getUuid(), result.getDepartment().getUuid());

      // Go through each of the departments.
      for (int i = 0; i < dto.getDepartments().size(); i++) {
        assertEquals(
          dto.getDepartments().get(i).getUuid(),
          result.getDepartments().get(i).getUuid()
        );
      }
    }
  }

  @Test
  public void create_ValidResource_ResourceCreated() {
    // Create person record with department data.
    PersonDTO persistedRecord = createTestData(true);

    // Ensure record was properly created and mapped over.
    Person result = databaseSupportService.findUnique(Person.class, "uuid", persistedRecord.getUuid());
    assertNotNull(result);
    assertEquals(result.getUuid(), persistedRecord.getUuid());
    assertEquals(result.getName(), persistedRecord.getName());

    // Check if associations exist with the correct UUID.
    assertNotNull(result.getDepartment());
    assertNotNull(result.getDepartments());
    assertEquals(result.getDepartment().getUuid(), persistedRecord.getDepartment().getUuid());
    assertEquals(result.getDepartments().get(1).getUuid(), persistedRecord.getDepartments().get(1).getUuid());

    // Clean up person record and all department data.
    cleanUpTestData(persistedRecord);
  }

  @Test
  public void findOne_ResourceAndRelations_FindsResourceAndRelations() {
    // Create person record with department data.
    PersonDTO persistedRecord = createTestData(true);

    // Create a query to find the persisted record. Including department data.
    QuerySpec querySpec = new QuerySpec(PersonDTO.class);
    querySpec.setIncludedRelations(createIncludeRelationSpecs("department", "departments"));
    PersonDTO result = personRepository.findOne(persistedRecord.getUuid(), querySpec);
    assertEqualsPersonDtos(persistedRecord, result, true);

    // Clean up person record and all department data.
    cleanUpTestData(persistedRecord);
  }

  @Test
  public void findOne_NoResourceFound_ThrowsResourceNotFoundException() {
    assertThrows(
      ResourceNotFoundException.class,
      () -> personRepository.findOne(UUID.randomUUID(), new QuerySpec(PersonDTO.class))
    );
  }

  @Test
  public void findAll_ResourceAndRelations_FindsResourceAndRelations() {
    Map<UUID, PersonDTO> expectedPersons = new HashMap<>();

    for (int i = 0; i < 10; i++) {
      // Create person record with department data.
      PersonDTO persistedRecord = createTestData(true);
      expectedPersons.put(persistedRecord.getUuid(), persistedRecord);
    }

    QuerySpec querySpec = new QuerySpec(PersonDTO.class);
    querySpec.setIncludedRelations(createIncludeRelationSpecs("department", "departments"));
    List<PersonDTO> resultList = personRepository.findAll(null, querySpec);

    assertEquals(expectedPersons.size(), resultList.size());

    resultList.forEach(resultElement -> {
      PersonDTO expectedDto = expectedPersons.get(resultElement.getUuid());
      assertEqualsPersonDtos(expectedDto, resultElement, true);

      // Clean up person record.
      cleanUpTestData(resultElement);
    });
  }

  @Test
  public void findAll_FilterByIds_FindsById() {
    List<PersonDTO> notIncludedRecords = new ArrayList<>();
    List<Serializable> idList = new ArrayList<>();

    for (int i = 0; i < 10; i++) {
      PersonDTO dto = createTestData(false);
      idList.add(dto.getUuid());

      // Persist a record not to be included in the findAll query.
      notIncludedRecords.add(createTestData(false));
    }

    List<PersonDTO> resultList = personRepository.findAll(idList, new QuerySpec(PersonDTO.class));

    assertEquals(idList.size(), resultList.size());
    resultList.forEach(resultElement -> {
      assertTrue(idList.contains(resultElement.getUuid()));

      // Clean up person record.
      cleanUpTestData(resultElement);
    });

    // Clean up the not included records
    cleanUpPersonList(notIncludedRecords);
  }

  @Test
  public void findAll_FilterWithRSQL_FiltersOnRSQL() {
    List<PersonDTO> notIncludedRecords = new ArrayList<>();
    List<PersonDTO> includedRecords = new ArrayList<>();

    String expectedName = RandomStringUtils.random(10);
    int expectedNumberOfResults = 10;

    for (int i = 0; i < expectedNumberOfResults; i++) {
      // Persist a specific name.
      PersonDTO dto = PersonDTO.builder().name(expectedName).group(RandomStringUtils.randomAlphabetic(4)).build();
      includedRecords.add(personRepository.create(dto));

      // Persist a record not to be included in the findAll query.
      notIncludedRecords.add(createTestData(false));
    }

    QuerySpec querySpec = new QuerySpec(PersonDTO.class);
    querySpec.addFilter(PathSpec.of("rsql").filter(FilterOperator.EQ, "name==" + expectedName));

    List<PersonDTO> resultList = personRepository.findAll(null, querySpec);
    assertEquals(expectedNumberOfResults, resultList.size());
    resultList.forEach(result -> assertEquals(expectedName, result.getName()));

    // Clean up persisted records.
    cleanUpPersonList(includedRecords);
    cleanUpPersonList(notIncludedRecords);
  }

  @Test
  public void findAll_FilterOnFieldEquals_FiltersOnField() {
    List<PersonDTO> notIncludedRecords = new ArrayList<>();
    List<PersonDTO> includedRecords = new ArrayList<>();

    String expectedName = RandomStringUtils.random(10);
    int expectedNumberOfResults = 10;

    for (int i = 0; i < expectedNumberOfResults; i++) {
      // Persist a specific name.
      PersonDTO dto = PersonDTO.builder().name(expectedName).group(RandomStringUtils.randomAlphabetic(4)).build();
      includedRecords.add(personRepository.create(dto));

      // Persist a record not to be included in the findAll query.
      notIncludedRecords.add(createTestData(false));
    }

    QuerySpec querySpec = new QuerySpec(PersonDTO.class);
    querySpec.addFilter(PathSpec.of("name").filter(FilterOperator.EQ, expectedName));

    List<PersonDTO> resultList = personRepository.findAll(null, querySpec);
    assertEquals(expectedNumberOfResults, resultList.size());
    resultList.forEach(result -> assertEquals(expectedName, result.getName()));

    // Clean up persisted records.
    cleanUpPersonList(includedRecords);
    cleanUpPersonList(notIncludedRecords);
  }

  @Test
  public void findAll_FilterOnNestedFieldEquals_FiltersOnNestedField() {
    PersonDTO expected = createTestData(true);
    List<PersonDTO> notIncludedRecords = new ArrayList<>();

    // Persist extra people with no department
    for (int i = 0; i < 10; i++) {
      // Persist a record not to be included in the findAll query.
      notIncludedRecords.add(createTestData(false));
    }

    QuerySpec querySpec = new QuerySpec(PersonDTO.class);
    querySpec.addFilter(
        PathSpec.of("department", "uuid").filter(
            FilterOperator.EQ,
            expected.getDepartment().getUuid()));

    List<PersonDTO> resultList = personRepository.findAll(null, querySpec);
    assertEquals(1, resultList.size());
    assertEqualsPersonDtos(expected, resultList.get(0), false);

    // Clean up persisted records.
    cleanUpTestData(expected);
    cleanUpPersonList(notIncludedRecords);
  }

  @Test
  public void findAll_FilterOnCustomField() {
    List<PersonDTO> persisted = new ArrayList<>();
    for (int i = 0; i < 10; i++) {
      persisted.add(createTestData(false));
    }

    QuerySpec querySpec = new QuerySpec(PersonDTO.class);
    PersonDTO expected = persisted.get(0);
    querySpec.addFilter(
      PathSpec.of("customField").filter(FilterOperator.EQ, expected.getCustomField()));

    List<PersonDTO> resultList = personRepository.findAll(null, querySpec);
    assertEquals(1, resultList.size());
    assertEquals(expected.getUuid(), resultList.get(0).getUuid());

    // Clean up the persisted records.
    cleanUpPersonList(persisted);
  }

  @Test
  public void findAll_FilterOnNestedCustomField() {
    PersonDTO expected = createTestData(true);
    List<PersonDTO> notIncludedRecords = new ArrayList<>();
    Department department = databaseSupportService.findUnique(Department.class, "uuid", expected.getDepartment().getUuid());

    // Persist extra people with no department
    for (int i = 0; i < 10; i++) {
      notIncludedRecords.add(createTestData(false));
    }

    QuerySpec querySpec = new QuerySpec(PersonDTO.class);
    querySpec.addFilter(PathSpec.of("department", "derivedFromLocation")
        .filter(FilterOperator.EQ, department.getLocation()));

    List<PersonDTO> resultList = personRepository.findAll(null, querySpec);
    assertEquals(1, resultList.size());
    assertEquals(expected.getUuid(), resultList.get(0).getUuid());

    // Clean up the persisted records.
    cleanUpTestData(expected);
    cleanUpPersonList(notIncludedRecords);
  }

  @Test
  public void findAll_SortingByName_ReturnsSorted() {
    List<String> names = Arrays.asList("a", "b", "c", "d");
    List<String> shuffledNames = Arrays.asList("b", "a", "d", "c");

    for (String name : shuffledNames) {
      // Persist a specific name.
      PersonDTO dto = PersonDTO.builder().name(name).build();
      personRepository.create(dto);
    }

    QuerySpec querySpec = new QuerySpec(PersonDTO.class);
    querySpec.setSort(Collections.singletonList(
        new SortSpec(Collections.singletonList("name"), Direction.ASC)));

    List<PersonDTO> resultList = personRepository.findAll(null, querySpec);
    for (int i = 0; i < names.size(); i++) {
      assertEquals(names.get(i), resultList.get(i).getName());
    }

    // Clean up the persisted records.
    cleanUpPersonList(resultList);
  }

  @Test
  public void findAll_SortingByName_ReturnsSortedCaseSensitiveOrNot() {
    List<String> shuffledNames = Arrays.asList("b", "a", "d", "C");
    List<Integer> matchingRooms = Arrays.asList(6, 2, 1, 11);

    List<String> namesCaseInsensitive = Arrays.asList("a", "b", "C", "d");
    List<String> namesCaseSensitive = Arrays.asList("C", "a", "b", "d");
    List<String> byRoom = Arrays.asList("d", "a", "b", "C");

    for (int i=0; i < shuffledNames.size(); i++) {
      // Persist a specific name.
      PersonDTO dto = PersonDTO.builder()
          .name(shuffledNames.get(i))
          .room(matchingRooms.get(i))
          .build();
      personRepository.create(dto);
    }

    QuerySpec querySpec = new QuerySpec(PersonDTO.class);
    querySpec.setSort(Collections.singletonList(
        new SortSpec(Collections.singletonList("name"), Direction.ASC)));

    //case insensitive by default
    List<PersonDTO> resultList = personRepository.findAll(null, querySpec);
    for (int i = 0; i < namesCaseInsensitive.size(); i++) {
      assertEquals(namesCaseInsensitive.get(i), resultList.get(i).getName());
    }

    //test case sensitive
    personRepository.setCaseSensitiveOrderBy(true);
    resultList = personRepository.findAll(null, querySpec);
    for (int i = 0; i < namesCaseSensitive.size(); i++) {
      assertEquals(namesCaseSensitive.get(i), resultList.get(i).getName());
    }

    // revert to default
    personRepository.setCaseSensitiveOrderBy(false);

    // sorting on non text field should have no effect
    querySpec.setSort(Collections.singletonList(
        new SortSpec(Collections.singletonList("room"), Direction.ASC)));
    resultList = personRepository.findAll(null, querySpec);
    for (int i = 0; i < namesCaseSensitive.size(); i++) {
      assertEquals(byRoom.get(i), resultList.get(i).getName());
    }

    // Clean up the persisted records.
    cleanUpPersonList(resultList);
  }

  @Test
  public void findAll_SortingByNestedProperty_ReturnsSorted() {
    List<String> names = Arrays.asList("a", "b", "c", "d");
    List<String> shuffledNames = Arrays.asList("b", "a", "d", "c");

    for (String name : shuffledNames) {
      DepartmentDto departmentDto = DepartmentDto.builder()
          .name(name)
          .location(RandomStringUtils.randomAlphabetic(5))
          .build();

      PersonDTO personDTO = PersonDTO.builder()
          .name(RandomStringUtils.randomAlphabetic(5))
          .department(departmentRepository.create(departmentDto))
          .build();
      personRepository.create(personDTO);
    }

    QuerySpec querySpec = new QuerySpec(PersonDTO.class);
    querySpec.setIncludedRelations(createIncludeRelationSpecs("department"));
    querySpec.setSort(
        Collections.singletonList(new SortSpec(Arrays.asList("department", "name"), Direction.ASC)));

    List<PersonDTO> resultList = personRepository.findAll(null, querySpec);
    for (int i = 0; i < names.size(); i++) {
      assertEquals(names.get(i), resultList.get(i).getDepartment().getName());
    }

    // Clean up the persisted records.
    cleanUpPersonList(resultList);
  }

  @Test
  public void findAll_SortingByNestedProperty_ReturnsResourcesWithNullProperty() {
    for (int i = 0; i < 3; i++) {
      personRepository.create(createTestData(false));
    }

    QuerySpec querySpec = new QuerySpec(PersonDTO.class);
    querySpec.setSort(Collections.singletonList(
      new SortSpec(Arrays.asList("department", "name"), Direction.ASC)));

    List<PersonDTO> resultList = personRepository.findAll(null, querySpec);
    Assertions.assertTrue(CollectionUtils.isNotEmpty(resultList), "no results were returned");
    resultList.forEach(result -> Assertions.assertNull(result.getDepartment()));

    // Clean up the persisted records.
    cleanUpPersonList(resultList);
  }

  @Test
  public void findAll_whenPageLimitIsSet_pageSizeIsLimited() {
    long pageLimit = 10;

    for (int i = 0; i < pageLimit * 2; i++) {
      persistPerson();
    }

    QuerySpec querySpec = new QuerySpec(PersonDTO.class);
    querySpec.setLimit(pageLimit);

    List<PersonDTO> result = personRepository.findAll(null, querySpec);
    assertEquals(pageLimit, result.size());
  }

  @Test
  public void findAll_whenPageOffsetIsSet_pageStartsAfterOffset() {
    long offset = 2;
    List<PersonDTO> dtos = new ArrayList<>();

    for (int i = 0; i < 10; i++) {
      PersonDTO dto = persistPerson();
      dtos.add(dto);
    }

    QuerySpec querySpec = new QuerySpec(PersonDTO.class);
    querySpec.setOffset(offset);
    List<PersonDTO> result = personRepository.findAll(null, querySpec);

    List<PersonDTO> expectedDtos = dtos.subList((int) offset, dtos.size());
    for (int i = 0; i < expectedDtos.size(); i++) {
      assertEqualsPersonDtos(expectedDtos.get(i), result.get(i), false);
    }
  }

  @Test
  public void findAll_FilterByIds_ReturnsTotalCount() {
    List<Serializable> idList = new ArrayList<>();

    for (int i = 0; i < 10; i++) {
      PersonDTO dto = persistPerson();
      idList.add(dto.getUuid());
      // Persist extra person not in list
      persistPerson();
    }

    ResourceList<PersonDTO> resultList = personRepository.findAll(idList, new QuerySpec(PersonDTO.class));
    PagedMetaInformation metadata = (PagedMetaInformation) resultList.getMeta();

    assertEquals(idList.size(), metadata.getTotalResourceCount());
  }

  @Test
  public void findAll_whenPageLimitIsSet_ReturnsTotalCount() {
    long pageLimit = 10;
    long totalResouceCount = pageLimit * 2;

    for (int i = 0; i < totalResouceCount; i++) {
      persistPerson();
    }

    QuerySpec querySpec = new QuerySpec(PersonDTO.class);
    querySpec.setLimit(pageLimit);

    ResourceList<PersonDTO> result = personRepository.findAll(null, querySpec);
    PagedMetaInformation metadata = (PagedMetaInformation) result.getMeta();
    assertEquals(totalResouceCount, metadata.getTotalResourceCount());
  }

  @Test
  public void findAll_NothingPersisted_ReturnsEmpty() {
    List<PersonDTO> result = personRepository.findAll(null, new QuerySpec(PersonDTO.class));
    assertEquals(0, result.size());
  }

  @Test
  /** Tests the fix from #20605 for an error on nested include relations. */
  public void findAll_whenNestedIncludeRequested_noErrorThrown() {
    QuerySpec querySpec = new QuerySpec(PersonDTO.class);
    querySpec.setIncludedRelations(
      List.of(
        new IncludeRelationSpec(List.of("department")),
        new IncludeRelationSpec(List.of("department", "departmentHead")),
        new IncludeRelationSpec(List.of("department", "departmentHead", "department"))
      ));

    personRepository.findAll(querySpec);
  }

  @Test
  public void save_UpdateAllFields_AllFieldsUpdated() {
    Department expectedDept = persistDepartment();
    DepartmentDto newDepart = DepartmentDto.builder().uuid(expectedDept.getUuid()).build();

    List<Department> expectedDepts = persistDepartments();
    List<DepartmentDto> newDepartments = expectedDepts.stream()
      .map(d -> DepartmentDto.builder().uuid(d.getUuid()).build())
      .collect(Collectors.toList());

    String expectedName = "new name";
    String[] expectedNickNames = Arrays.asList("new", "nick", "names").toArray(new String[0]);

    PersonDTO dto = persistPerson();

    dto.setName(expectedName);
    dto.setNickNames(expectedNickNames);
    dto.setDepartments(newDepartments);
    dto.setDepartment(newDepart);

    personRepository.save(dto);

    Person result = baseDAO.findOneByNaturalId(dto.getUuid(), Person.class);
    assertEqualsPersonDtoAndEntity(dto, result, expectedDept, expectedDepts);
  }

  @Test
  public void save_NullAllFields_AllFieldsNulled() {
    PersonDTO dto = createTestData(false);
    dto.setName(null);
    dto.setNickNames(null);
    dto.setDepartment(null);
    dto.setDepartments(null);

    personRepository.save(dto);

    Person result = databaseSupportService.findUnique(Person.class, "uuid", dto.getUuid());
    assertNull(result.getName());
    assertNull(result.getNickNames());
    assertNull(result.getDepartment());
    assertNull(result.getDepartments());
  }

  @Test
  public void save_NoResourceFound_ThrowsResourceNotFoundException() {
    assertThrows(ResourceNotFoundException.class, () -> personRepository.save(PersonDTO.builder()
        .name(RandomStringUtils.randomAlphabetic(10))
        .department(DepartmentDto.builder()
            .uuid(UUID.randomUUID()) // Department UUID does not exist.
            .build()
        )
        .build()
    ));
  }

  @Test
  public void delete_ValidResource_ResourceRemoved() {
    PersonDTO dto = persistPerson();

    assertNotNull(baseDAO.findOneByNaturalId(dto.getUuid(), Person.class));

    personRepository.delete(dto.getUuid());
    assertNull(baseDAO.findOneByNaturalId(dto.getUuid(), Person.class));
  }

  @Test
  public void delete_NoResourceFound_ThrowsResourceNotFoundException() {
    assertThrows(ResourceNotFoundException.class, () -> personRepository.delete(UUID.randomUUID()));
  }

  @Test
  public void getMetaInformation_whenBuildVersionIsSet_buildVersionIncludedInMeta() {
    DinaMetaInfo meta = personRepository.getMetaInformation(
      List.of(),
      new QuerySpec(PersonDTO.class),
      new DinaMetaInfo()
    );

    assertEquals("test-api-version", meta.getModuleVersion());
  }

  private static void assertEqualsPersonDtoAndEntity(
    PersonDTO dto,
    Person entity,
    Department expectedDepartment,
    List<Department> expectedDepartments
  ) {
    assertEquals(dto.getUuid(), entity.getUuid());
    assertEquals(dto.getName(), entity.getName());
    assertArrayEquals(dto.getNickNames(), entity.getNickNames());
    assertTrue(EqualsBuilder.reflectionEquals(expectedDepartment, entity.getDepartment()));
    assertEquals(expectedDepartments, entity.getDepartments());
    //assertThat(expectedDepartments, Is.is(entity.getDepartments()));
  }

  private PersonDTO persistPerson() {
    // Persist data for departments, which will be used by the person.
    singleRelationUnderTest = persistDepartment();
    collectionRelationUnderTest = persistDepartments();

    PersonDTO dto = createPersonDto();
    return personRepository.create(dto);
  }

  private PersonDTO createPersonDto() {
    DepartmentDto singleRelationDto = DepartmentDto.builder()
      .uuid(singleRelationUnderTest.getUuid())
      .build();
    List<DepartmentDto> collectionRelationDtos = collectionRelationUnderTest.stream()
      .map(c -> DepartmentDto.builder().uuid(c.getUuid()).build())
      .collect(Collectors.toList());
    return PersonDTO.builder()
      .department(singleRelationDto)
      .departments(collectionRelationDtos)
      .nickNames(Arrays.asList("d", "z", "q").toArray(new String[0]))
      .name(RandomStringUtils.randomAlphabetic(4))
      .group(RandomStringUtils.randomAlphabetic(4))
      .build();
  }

  private static Department createDepartment(String name, String Location) {
    Department depart = Department.builder()
      .uuid(UUID.randomUUID())
      .name(name)
      .location(Location)
      .build();
    return depart;
  }

  private Department persistDepartment() {
    Department depart = createDepartment(RandomStringUtils.random(4), RandomStringUtils.random(4));
    baseDAO.create(depart);
    return depart;
  }

  private List<Department> persistDepartments() {
    List<Department> departments = new ArrayList<>();
    for (int i = 0; i < 10; i++) {
      departments.add(persistDepartment());
    }
    return departments;
  }

  private static List<IncludeRelationSpec> createIncludeRelationSpecs(String... args) {
    return Arrays.asList(args).stream()
      .map(Arrays::asList)
      .map(IncludeRelationSpec::new)
      .collect(Collectors.toList());
  }
}
