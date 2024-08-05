package ca.gc.aafc.dina.repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.NonNull;

import org.springframework.boot.info.BuildProperties;

import com.fasterxml.jackson.databind.ObjectMapper;

import ca.gc.aafc.dina.entity.DinaEntity;
import ca.gc.aafc.dina.filter.DinaFilterResolver;
import ca.gc.aafc.dina.filter.FilterComponent;
import ca.gc.aafc.dina.filter.QueryComponent;
import ca.gc.aafc.dina.filter.QueryStringParser;
import ca.gc.aafc.dina.mapper.DinaMapper;
import ca.gc.aafc.dina.repository.external.ExternalResourceProvider;
import ca.gc.aafc.dina.security.auth.DinaAuthorizationService;
import ca.gc.aafc.dina.service.AuditService;
import ca.gc.aafc.dina.service.DinaService;

public class DinaRepositoryV2<D,E extends DinaEntity> {

  private final DinaAuthorizationService authorizationService;
  private final DinaService<E> dinaService;
  private final Class<E> entityClass;

  public DinaRepositoryV2(@NonNull DinaService<E> dinaService,
                          @NonNull DinaAuthorizationService authorizationService,
                          @NonNull Optional<AuditService> auditService,
                          @NonNull DinaMapper<D, E> dinaMapper,
                          @NonNull Class<D> resourceClass,
                          @NonNull Class<E> entityClass,
                          DinaFilterResolver filterResolver,
                          ExternalResourceProvider externalResourceProvider,
                          @NonNull BuildProperties buildProperties,
                          ObjectMapper objMapper) {

    this.authorizationService = authorizationService;
    this.dinaService = dinaService;
    this.entityClass = entityClass;

  }

  public D getOne(UUID identifier, String queryString) {

    QueryComponent queryComponents = QueryStringParser.parse(queryString);
    FilterComponent fc = queryComponents.getFilters();

    //
  //  dinaService.findAll()
    E entity = dinaService.findOne(identifier, entityClass, Set.of());
    authorizationService.authorizeRead(entity);

    return null;

  }

  public List<D> getAll(String queryString) {

    QueryComponent queryComponents = QueryStringParser.parse(queryString);
    FilterComponent fc = queryComponents.getFilters();

    //
    //  dinaService.findAll()
   // E entity = dinaService.findOne(identifier, entityClass, Set.of());

    return null;

  }
}
