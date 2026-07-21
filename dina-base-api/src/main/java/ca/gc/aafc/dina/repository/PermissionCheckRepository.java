package ca.gc.aafc.dina.repository;

import org.springframework.hateoas.RepresentationModel;
import org.springframework.http.ResponseEntity;

import ca.gc.aafc.dina.dto.PermissionCheckDto;
import ca.gc.aafc.dina.exception.ResourceNotFoundException;
import ca.gc.aafc.dina.jsonapi.JsonApiDocument;
import ca.gc.aafc.dina.security.TextHtmlSanitizer;
import ca.gc.aafc.dina.security.auth.PermissionAuthorizationService;

import static com.toedter.spring.hateoas.jsonapi.JsonApiModelBuilder.jsonApiModel;

import java.net.URI;
import java.util.List;
import java.util.UUID;

public class PermissionCheckRepository {

  // inject all DinaRepositoryV2
  private final List<DinaRepositoryV2<?, ?>> dinaRepositories;

  public PermissionCheckRepository(List<DinaRepositoryV2<?, ?>> dinaRepositories) {
    this.dinaRepositories = dinaRepositories;
  }

  /**
   * Called by a POST
   *
   * @param docToCheck
   * @return
   * @throws ResourceNotFoundException
   */
  public ResponseEntity<RepresentationModel<?>> handleCheckPermissions(JsonApiDocument docToCheck)
      throws ResourceNotFoundException {
    DinaRepositoryV2<?, ?> repo = resolveRepository(docToCheck.getType());

    if (repo == null) {
      throw ResourceNotFoundException.create(PermissionCheckDto.TYPE_NAME,
        TextHtmlSanitizer.sanitizeText(docToCheck.getType()));
    }

    PermissionCheckDto dto;
    if (repo.getAuthorizationService() instanceof PermissionAuthorizationService permissionAuthorizationService) {
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
    return ResponseEntity.created(uri)
      .body(jsonApiModel().model(RepresentationModel.of(dto)).build());
  }

  private DinaRepositoryV2<?, ?> resolveRepository(String jsonApiType) {

    // Find the repository that has the matching json api type
    for (DinaRepositoryV2<?, ?> repository : dinaRepositories) {
      if (repository.isForJsonApiType(jsonApiType)) {
        return repository;
      }
    }
    return null;
  }
}
