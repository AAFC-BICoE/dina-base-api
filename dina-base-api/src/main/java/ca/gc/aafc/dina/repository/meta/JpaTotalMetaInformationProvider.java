package ca.gc.aafc.dina.repository.meta;

import java.util.function.Function;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.From;

import ca.gc.aafc.dina.mapper.JpaDtoMapper;
import io.crnk.core.resource.meta.DefaultPagedMetaInformation;
import lombok.RequiredArgsConstructor;

/**
 * Uses an existing JPA criteria query to provide a DefaultPagedMetaInformation containing the total
 * count of resources that match the existing query's restrictions.
 * @deprecated will be removed with JPA repository classes
 */
@RequiredArgsConstructor
@Deprecated(forRemoval = true, since = "0.42")
public class JpaTotalMetaInformationProvider implements JpaMetaInformationProvider {

  private final EntityManager entityManager;
  
  private final JpaDtoMapper jpaDtoMapper;

  @Override
  public DefaultPagedMetaInformation getMetaInformation(JpaMetaInformationParams params) {
    CriteriaBuilder cb = entityManager.getCriteriaBuilder();
    
    // Create the total count query.
    CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
    From<?, ?> sourcePath = countQuery.from(
        jpaDtoMapper.getEntityClassForDto(params.getSourceResourceClass())
    );
    
    Function<From<?, ?>, From<?, ?>> customRoot = params.getCustomRoot();
    From<?, ?> targetPath = customRoot != null ? customRoot.apply(sourcePath) : sourcePath;
    
    countQuery.select(cb.count(targetPath));

    // Use the same restrictions as the existing entity query.
    if (params.getCustomFilter() != null) {
      countQuery.where(params.getCustomFilter().apply(targetPath, countQuery, cb));
    }

    // Get the total.
    Long total = this.entityManager.createQuery(countQuery).getSingleResult();

    // Return the DefaultPagedMetaInformation with the total.
    DefaultPagedMetaInformation metaInformation = new DefaultPagedMetaInformation();
    metaInformation.setTotalResourceCount(total);
    return metaInformation;
  }

}
