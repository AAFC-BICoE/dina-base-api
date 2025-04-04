package ca.gc.aafc.dina.service;

import ca.gc.aafc.dina.entity.ManagedAttribute;
import ca.gc.aafc.dina.jpa.BaseDAO;
import ca.gc.aafc.dina.jpa.PredicateHelper;
import ca.gc.aafc.dina.vocabulary.VocabularyKeyHelper;

import lombok.NonNull;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.validation.SmartValidator;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Predicate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Base Managed Attribute Service that takes care of the generation of the key.
 *
 * @param <T>
 */
public abstract class ManagedAttributeService<T extends ManagedAttribute>
  extends DefaultDinaService<T> {
  private static final String KEY = "key";
  private final Class<T> maClass;

  public ManagedAttributeService(BaseDAO baseDAO, SmartValidator smartValidator, @NonNull Class<T> managedAttributeClass) {
    super(baseDAO, smartValidator);
    this.maClass = managedAttributeClass;
  }

  @Override
  protected void preCreate(T entity) {
    if (StringUtils.isNotBlank(entity.getName())) {
      entity.setKey(VocabularyKeyHelper.generateKeyFromName(entity.getName()));
    }
  }

  @Override
  protected void preDelete(T entity) {
    throw new UnsupportedOperationException("DELETE");
  }

  public Map<String, T> findAttributesForKeys(Set<String> keySet) {
    return findAttributesForKeys(keySet, null);
  }

  public Map<String, T> findAttributesForKeys(Set<String> keySet, Pair<String, Object> andClause) {
    if (CollectionUtils.isEmpty(keySet)) {
      return Map.of();
    }
    return this.findAll(
        maClass, (criteriaBuilder, eRoot) -> {
          CriteriaBuilder.In<String> in = criteriaBuilder.in(eRoot.get(KEY));
          keySet.forEach(in::value);

          if (andClause == null) {
            return new Predicate[]{in};
          }

          return new Predicate[]{PredicateHelper.appendPropertiesEqual(in, criteriaBuilder, eRoot, List.of(andClause))};
        },
        null, 0, Integer.MAX_VALUE
    ).stream().collect(Collectors.toMap(ManagedAttribute::getKey, Function.identity()));
  }

  public T findOneByKey(String key) {
    return findOneByProperty(maClass, KEY, key);
  }

  /**
   * Find a managed attribute by key and another property that is part of the unique constraint.
   * @param key
   * @param andClause clause that should be added to
   * @return
   */
  public T findOneByKeyAnd(String key, Pair<String, Object> andClause) {
    return findOneByProperties(maClass,
        List.of(Pair.of(KEY, key), andClause));
  }
}
