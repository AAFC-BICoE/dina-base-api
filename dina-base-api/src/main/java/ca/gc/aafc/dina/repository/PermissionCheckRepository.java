package ca.gc.aafc.dina.repository;

import org.springframework.context.ApplicationContext;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Repository;

import ca.gc.aafc.dina.dto.PermissionCheckDto;
import ca.gc.aafc.dina.exception.ResourceNotFoundException;
import ca.gc.aafc.dina.jsonapi.JsonApiDocument;
import ca.gc.aafc.dina.security.TextHtmlSanitizer;
import ca.gc.aafc.dina.security.auth.PermissionAuthorizationService;

import static com.toedter.spring.hateoas.jsonapi.JsonApiModelBuilder.jsonApiModel;

import jakarta.inject.Inject;
import java.net.URI;
import java.util.Map;
import java.util.UUID;

@Repository
public class PermissionCheckRepository {

  @Inject
  private ApplicationContext context;

  /**
   * Called by a POST
   * @param docToCheck
   * @return
   * @throws ResourceNotFoundException
   */
  public ResponseEntity<RepresentationModel<?>> handleCheckPermissions(JsonApiDocument docToCheck) throws ResourceNotFoundException {
    DinaRepositoryV2<?,?> repo = resolveRepository(docToCheck.getType());

    if (repo == null) {
      throw ResourceNotFoundException.create(PermissionCheckDto.TYPE_NAME, TextHtmlSanitizer.sanitizeText(docToCheck.getType()));
    }

    PermissionCheckDto dto;
    if(repo.getAuthorizationService() instanceof PermissionAuthorizationService permissionAuthorizationService) {
      dto = PermissionCheckDto.builder()
        .id(UUID.randomUUID().toString())
        .targetType(docToCheck.getType())
        .permissions(repo.checkPermissions(docToCheck))
        .permissionsProvider(permissionAuthorizationService.getName())
        .evaluatedAttributes(permissionAuthorizationService.evaluatedAttribute())
        .build();
    } else {
      throw new IllegalStateException("PermissionAuthorizationService instance required");
    }

    URI uri = URI.create(PermissionCheckDto.TYPE_NAME);
    return ResponseEntity.created(uri).body(jsonApiModel().model(RepresentationModel.of(dto)).build());
  }

  private DinaRepositoryV2<?, ?> resolveRepository(String jsonApiType) {
    // Get all beans of type DinaRepositoryV2
    Map<String, DinaRepositoryV2> repositories = context.getBeansOfType(DinaRepositoryV2.class);
    // Find the repository that has the matching json api type
    for (DinaRepositoryV2<?, ?> repository : repositories.values()) {
      if (repository.isForJsonApiType(jsonApiType)) {
        return repository;
      }
    }
    return null;
  }
}
