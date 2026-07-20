package ca.gc.aafc.dina.repository;

import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Repository;

import jakarta.inject.Inject;
import java.util.Map;

import ca.gc.aafc.dina.jsonapi.JsonApiDocument;

@Repository
public class PermissionCheckRepository {

  @Inject
  private ApplicationContext context;

  public void checkPermissions(JsonApiDocument docToCheck) {
    DinaRepositoryV2<?,?> repo = resolveRepository(docToCheck.getType());

    // return that in the proper meta block
    repo.checkPermissions(docToCheck);
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
