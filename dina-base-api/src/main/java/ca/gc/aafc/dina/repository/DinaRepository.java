package ca.gc.aafc.dina.repository;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import ca.gc.aafc.dina.entity.DinaEntity;
import ca.gc.aafc.dina.jpa.DinaService;
import ca.gc.aafc.dina.mapper.DerivedDtoField;
import ca.gc.aafc.dina.mapper.DinaMapper;
import io.crnk.core.engine.information.resource.ResourceField;
import io.crnk.core.engine.information.resource.ResourceInformation;
import io.crnk.core.engine.internal.utils.PropertyUtils;
import io.crnk.core.engine.registry.ResourceRegistry;
import io.crnk.core.engine.registry.ResourceRegistryAware;
import io.crnk.core.queryspec.QuerySpec;
import io.crnk.core.repository.ResourceRepository;
import io.crnk.core.resource.list.ResourceList;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.SneakyThrows;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class DinaRepository<D, E extends DinaEntity>
    implements ResourceRepository<D, Serializable>, ResourceRegistryAware {

  @NonNull
  private final DinaService<E> dinaService;

  @NonNull
  private final DinaMapper<D, E> dinaMapper;

  @Getter
  @NonNull
  private final Class<D> resourceClass;

  @NonNull
  private final Class<E> entityClass;

  @Getter
  @Setter(onMethod_ = @Override)
  private ResourceRegistry resourceRegistry;

  @Override
  public D findOne(Serializable id, QuerySpec querySpec) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public ResourceList<D> findAll(QuerySpec querySpec) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public ResourceList<D> findAll(Collection<Serializable> ids, QuerySpec querySpec) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public <S extends D> S save(S resource) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  @SneakyThrows
  public <S extends D> S create(S resource) {
    E entity = entityClass.newInstance();

    Map<Class<?>, Set<String>> fieldsPerClass = parseFieldsPerClass(resourceClass, new HashMap<>());
    fieldsPerClass.forEach((clazz, fields) -> fields.removeIf(f -> isGenerated(clazz, f)));

    ResourceInformation resourceInformation =this.resourceRegistry
      .findEntry(resourceClass)
      .getResourceInformation();

    Set<String> relations = resourceInformation.getRelationshipFields()
      .stream()
      .map(af -> af.getUnderlyingName())
      .collect(Collectors.toSet());

    dinaMapper.applyDtoToEntity(resource, entity, fieldsPerClass, relations);

    linkRelations(entity, resourceInformation.getRelationshipFields());

    dinaService.create(entity);

    return null;
  }

  @Override
  public void delete(Serializable id) {
    // TODO Auto-generated method stub

  }

  private <T> Map<Class<?>, Set<String>> parseFieldsPerClass(Class<T> clazz,
      Map<Class<?>, Set<String>> fieldsPerClass) {
    Objects.requireNonNull(clazz);
    Objects.requireNonNull(fieldsPerClass);

    if (fieldsPerClass.containsKey(clazz)) {
      return fieldsPerClass;
    }

    ResourceInformation resourceInformation =this.resourceRegistry
      .findEntry(clazz)
      .getResourceInformation();

    Set<String> attributes = resourceInformation.getAttributeFields()
      .stream()
      .map(af -> af.getUnderlyingName())
      .collect(Collectors.toSet());
    attributes.add(resourceInformation.getIdField().getUnderlyingName());

    fieldsPerClass.put(clazz, attributes);

    List<ResourceField> relationFields = resourceInformation.getRelationshipFields();
    for (ResourceField relationField : relationFields) {
      parseFieldsPerClass(relationField.getElementType(), fieldsPerClass);
    }

    return fieldsPerClass;
  }

  private void linkRelations(E entity, List<ResourceField> relations) {
    Objects.requireNonNull(entity);
    Objects.requireNonNull(relations);

    for (ResourceField relationField : relations) {
      ResourceInformation relationInfo =this.resourceRegistry
        .findEntry(relationField.getElementType())
        .getResourceInformation();

      String fieldName = relationField.getUnderlyingName();
      String idFieldName = relationInfo.getIdField().getUnderlyingName();

      if (relationField.isCollection()) {
        Collection<?> relation = (Collection<?>) PropertyUtils.getProperty(entity, fieldName);
        if (relation != null) {
          Collection<?> mappedCollection = relation.stream()
              .map(rel -> returnPersistedObject(idFieldName, rel))
              .collect(Collectors.toList());
          PropertyUtils.setProperty(entity, fieldName, mappedCollection);
        }
      } else {
        Object relation = PropertyUtils.getProperty(entity, fieldName);
        if (relation != null) {
          Object persistedRelationObject = returnPersistedObject(idFieldName, relation);
          PropertyUtils.setProperty(entity, fieldName, persistedRelationObject);
        }
      }
    }
  }

  private Object returnPersistedObject(String idFieldName, Object object) {
    Object relationID = PropertyUtils.getProperty(object, idFieldName);
    Object persistedRelationObject = dinaService.findOne(relationID, object.getClass());
    return persistedRelationObject;
  }

  @SneakyThrows(NoSuchFieldException.class)
  private <T> boolean isGenerated(Class<T> clazz, String field) {
    return clazz.getDeclaredField(field).isAnnotationPresent(DerivedDtoField.class);
  }

}
