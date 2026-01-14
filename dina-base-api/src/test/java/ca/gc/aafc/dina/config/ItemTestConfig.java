package ca.gc.aafc.dina.config;

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

import ca.gc.aafc.dina.dto.ItemDto;
import ca.gc.aafc.dina.entity.Item;
import ca.gc.aafc.dina.exception.ResourceGoneException;
import ca.gc.aafc.dina.exception.ResourceNotFoundException;
import ca.gc.aafc.dina.exception.ResourcesGoneException;
import ca.gc.aafc.dina.exception.ResourcesNotFoundException;
import ca.gc.aafc.dina.jpa.BaseDAO;
import ca.gc.aafc.dina.jsonapi.JsonApiBulkDocument;
import ca.gc.aafc.dina.jsonapi.JsonApiBulkResourceIdentifierDocument;
import ca.gc.aafc.dina.jsonapi.JsonApiDocument;
import ca.gc.aafc.dina.mapper.ItemMapper;
import ca.gc.aafc.dina.repository.DinaRepositoryV2;
import ca.gc.aafc.dina.security.auth.AllowAllAuthorizationService;
import ca.gc.aafc.dina.service.AuditService;
import ca.gc.aafc.dina.service.DefaultDinaService;
import ca.gc.aafc.dina.service.DinaService;

import static com.toedter.spring.hateoas.jsonapi.MediaTypes.JSON_API_VALUE;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import java.util.Optional;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;
import javax.transaction.Transactional;
import lombok.NonNull;

/**
 * Configures Item repository and service.
 */
@TestConfiguration
public class ItemTestConfig {

  public static final String PATH = ItemDto.TYPE_NAME;

  @Service
  public static class ItemService extends DefaultDinaService<Item> {
    public ItemService(@NonNull BaseDAO baseDAO, SmartValidator sv) {
      super(baseDAO, sv);
    }
  }

  @RestController
  @RequestMapping(produces = JSON_API_VALUE)
  public static class ItemTestRepositoryV2 extends DinaRepositoryV2<ItemDto, Item> {

    public ItemTestRepositoryV2(
      DinaService<Item> dinaService,
      BuildProperties buildProperties,
      Optional<AuditService> auditService,
      ObjectMapper objMapper
    ) {
      super(dinaService, new AllowAllAuthorizationService(),
        auditService, ItemMapper.INSTANCE, ItemDto.class, Item.class,
        buildProperties, objMapper);
    }

    @Override
    protected Link generateLinkToResource(ItemDto dto) {
      try {
        return linkTo(
          methodOn(PersonTestConfig.PersonDinaTestRepositoryV2.class).onFindOne(dto.getUuid(), null)).withSelfRel();
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
