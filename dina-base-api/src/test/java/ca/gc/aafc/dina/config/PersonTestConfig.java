package ca.gc.aafc.dina.config;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;
import javax.transaction.Transactional;
import lombok.NonNull;

import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.validation.SmartValidator;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;

import ca.gc.aafc.dina.DinaUserConfig;
import ca.gc.aafc.dina.dto.DepartmentDto;
import ca.gc.aafc.dina.dto.PersonDTO;
import ca.gc.aafc.dina.entity.Department;
import ca.gc.aafc.dina.entity.Person;
import ca.gc.aafc.dina.exception.ResourceGoneException;
import ca.gc.aafc.dina.exception.ResourceNotFoundException;
import ca.gc.aafc.dina.exception.ResourcesGoneException;
import ca.gc.aafc.dina.exception.ResourcesNotFoundException;
import ca.gc.aafc.dina.jpa.BaseDAO;
import ca.gc.aafc.dina.jsonapi.JsonApiBulkDocument;
import ca.gc.aafc.dina.jsonapi.JsonApiBulkResourceIdentifierDocument;
import ca.gc.aafc.dina.jsonapi.JsonApiDocument;
import ca.gc.aafc.dina.mapper.DepartmentMapper;
import ca.gc.aafc.dina.mapper.PersonMapper;
import ca.gc.aafc.dina.repository.DinaRepositoryV2;
import ca.gc.aafc.dina.security.auth.AllowAllAuthorizationService;
import ca.gc.aafc.dina.service.AuditService;
import ca.gc.aafc.dina.service.DefaultDinaService;
import ca.gc.aafc.dina.service.DinaService;

import static com.toedter.spring.hateoas.jsonapi.MediaTypes.JSON_API_VALUE;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

/**
 * Configures Person and Department repository and service.
 */
@TestConfiguration
public class PersonTestConfig {

  public static final String PATH = PersonDTO.TYPE_NAME + "v2";
  public static final String EXPENSIVE_VALUE_TO_COMPUTE = "$$$$";
  public static final String AUGMENTED_DATA_VALUE = "VR Augmented";

  @Service
  public static class DinaPersonService extends DefaultDinaService<Person> {

    public DinaPersonService(@NonNull BaseDAO baseDAO, SmartValidator sv) {
      super(baseDAO, sv);
    }

    @Override
    protected void preCreate(Person entity) {
      entity.setUuid(UUID.randomUUID());
      entity.setGroup(standardizeGroupName(entity));
    }

    @Override
    public Person handleOptionalFields(Person entity, Map<String, List<String>> optionalFields) {
      if (optionalFields.getOrDefault(PersonDTO.TYPE_NAME, List.of())
        .contains("expensiveToCompute")) {
        entity.setExpensiveToCompute(EXPENSIVE_VALUE_TO_COMPUTE);
      }
      return entity;
    }

    @Override
    public void augmentEntity(Person entity, Set<String> relationships) {
      entity.setAugmentedData(AUGMENTED_DATA_VALUE);
    }
  }

  @RestController
  @RequestMapping(produces = JSON_API_VALUE)
  static class DepartmentTestRepositoryV2 extends DinaRepositoryV2<DepartmentDto, Department> {
    public DepartmentTestRepositoryV2(DinaUserConfig.DepartmentDinaService departmentDinaService,
                                      BuildProperties buildProperties,
                                      Optional<AuditService> auditService,
                                      ObjectMapper objMapper
    ) {
      super(
        departmentDinaService,
        new AllowAllAuthorizationService(),
        auditService,
        DepartmentMapper.INSTANCE,
        DepartmentDto.class,
        Department.class,
        buildProperties, objMapper);
    }
  }

  @RestController
  @RequestMapping(produces = JSON_API_VALUE)
  static class PersonDinaTestRepositoryV2 extends DinaRepositoryV2<PersonDTO, Person> {
    public PersonDinaTestRepositoryV2(
      DinaService<Person> dinaService,
      BuildProperties buildProperties,
      Optional<AuditService> auditService,
      ObjectMapper objMapper
    ) {
      super(dinaService, new AllowAllAuthorizationService(),
        auditService, PersonMapper.INSTANCE, PersonDTO.class, Person.class,
        buildProperties, objMapper);
    }

    @Override
    protected Link generateLinkToResource(PersonDTO dto) {
      try {
        return linkTo(
          methodOn(PersonDinaTestRepositoryV2.class).onFindOne(dto.getUuid(), null)).withSelfRel();
      } catch (ResourceNotFoundException | ResourceGoneException e) {
        throw new RuntimeException(e);
      }
    }

    @GetMapping(PATH + "/{id}")
    public ResponseEntity<RepresentationModel<?>> onFindOne(@PathVariable UUID id,
                                                            HttpServletRequest req)
      throws ResourceNotFoundException, ResourceGoneException {
      return handleFindOne(id, req);
    }

    @GetMapping(PATH)
    public ResponseEntity<RepresentationModel<?>> onFindAll(HttpServletRequest req) {
      return handleFindAll(req);
    }

    @PostMapping(path = PATH, consumes = JSON_API_BULK)
    @Transactional
    public ResponseEntity<RepresentationModel<?>> onBulkCreate(
      @RequestBody JsonApiBulkDocument jsonApiBulkDocument) {
      return handleBulkCreate(jsonApiBulkDocument, null);
    }

    @PostMapping(path = PATH + "/" + JSON_API_BULK_LOAD_PATH, consumes = JSON_API_BULK)
    public ResponseEntity<RepresentationModel<?>> onBulkLoad(
      @RequestBody JsonApiBulkResourceIdentifierDocument jsonApiBulkDocument, HttpServletRequest req)
      throws ResourcesNotFoundException, ResourcesGoneException {
      return handleBulkLoad(jsonApiBulkDocument, req);
    }

    @PostMapping(path = PATH)
    @Transactional
    public ResponseEntity<RepresentationModel<?>> onCreate(
      @RequestBody JsonApiDocument postedDocument) {

      return handleCreate(postedDocument, null);
    }

    @PatchMapping(PATH + "/{id}")
    @Transactional
    public ResponseEntity<RepresentationModel<?>> onUpdate(
      @RequestBody JsonApiDocument partialPatchDto,
      @PathVariable UUID id) throws ResourceNotFoundException, ResourceGoneException {
      return handleUpdate(partialPatchDto, id);
    }

    @PatchMapping(path = PATH, produces = JSON_API_BULK)
    @Transactional
    public ResponseEntity<RepresentationModel<?>> onBulkUpdate(
      @RequestBody JsonApiBulkDocument jsonApiBulkDocument)
      throws ResourceNotFoundException, ResourceGoneException {
      return handleBulkUpdate(jsonApiBulkDocument);
    }

    @DeleteMapping(path = PATH, produces = JSON_API_BULK)
    @Transactional
    public ResponseEntity<RepresentationModel<?>> onBulkDelete(@RequestBody
                                                               JsonApiBulkResourceIdentifierDocument jsonApiBulkDocument)
      throws ResourceNotFoundException, ResourceGoneException {
      return handleBulkDelete(jsonApiBulkDocument);
    }

    @DeleteMapping(PATH + "/{id}")
    @Transactional
    public ResponseEntity<RepresentationModel<?>> onDelete(@PathVariable UUID id)
      throws ResourceNotFoundException, ResourceGoneException {
      return handleDelete(id);
    }
  }
}
