package ca.gc.aafc.dina.repository.meta;

import java.util.function.Function;

import javax.annotation.Nullable;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.From;
import javax.persistence.criteria.Predicate;

import ca.gc.aafc.dina.util.TriFunction;
import io.crnk.core.resource.meta.MetaInformation;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;

/**
 * Interface for providing JSONAPI response meta information given a JPA Criteria Query.
 */
@Deprecated(forRemoval = true, since = "0.42")
public interface JpaMetaInformationProvider {

  /**
   * Gets the MetaInformation of a JPA/DTO resource query.
   * 
   * @param params The parameters for creating this MetaInformation.
   * @return the query's MetaInformation
   */
  MetaInformation getMetaInformation(JpaMetaInformationParams params);
  
  @Builder
  @Getter
  class JpaMetaInformationParams {
    
    /**
     * The source resource class used when querying across a relationship.
     */
    @NonNull
    private Class<?> sourceResourceClass;
    
    /**
     * The custom root function used when querying across a relationship.
     */
    @Nullable
    private Function<From<?, ?>, From<?, ?>> customRoot;
    
    /**
     * The custom filter used by the query.
     */
    @Nullable
    private TriFunction<From<?, ?>, CriteriaQuery<?>, CriteriaBuilder, Predicate> customFilter;
    
  }
  
}
