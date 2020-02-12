package ca.gc.aafc.dina.jpa;

import java.util.Map;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import lombok.NonNull;

/**
 * Maps DTOs to JPA entities.
 */
public class JpaDtoMapper {
  
  @NonNull
  private final BiMap<Class<?>, Class<?>> jpaEntities;
  
  public JpaDtoMapper(Map<Class<?>, Class<?>> jpaEntities) {
    this.jpaEntities = HashBiMap.create(jpaEntities);
  }
  
  public Class<?> getEntityClassForDto(Class<?> dtoClass) {
    return jpaEntities.get(dtoClass);
  }
  
  public Class<?> getDtoClassForEntity(Class<?> entityClass) {
    return jpaEntities.inverse().get(entityClass);
  }
  
}
