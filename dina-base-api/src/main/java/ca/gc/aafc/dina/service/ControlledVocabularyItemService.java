package ca.gc.aafc.dina.service;

import org.apache.commons.lang3.StringUtils;
import org.springframework.validation.SmartValidator;

import ca.gc.aafc.dina.entity.ControlledVocabulary;
import ca.gc.aafc.dina.entity.ControlledVocabularyItem;
import ca.gc.aafc.dina.jpa.BaseDAO;
import ca.gc.aafc.dina.util.UUIDHelper;
import ca.gc.aafc.dina.validation.ControlledVocabularyItemValidator;
import ca.gc.aafc.dina.vocabulary.VocabularyKeyHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Predicate;
import lombok.NonNull;

/**
 * Base ControlledVocabularyItem Service that takes care of the generation of the key.
 *
 * @param <T>
 */
public abstract class ControlledVocabularyItemService<T extends ControlledVocabularyItem> extends DefaultDinaService<T> {

  private final Class<T> clazz;
  private final ControlledVocabularyItemValidator itemValidator;

  public ControlledVocabularyItemService(BaseDAO baseDAO, SmartValidator smartValidator,
                                         @NonNull Class<T> clazz,
                                         ControlledVocabularyItemValidator itemValidator) {
    super(baseDAO, smartValidator);
    this.clazz = clazz;
    this.itemValidator = itemValidator;
  }

  @Override
  protected void preCreate(ControlledVocabularyItem entity) {
    if (StringUtils.isNotBlank(entity.getName())) {
      entity.setKey(VocabularyKeyHelper.generateKeyFromName(entity.getName()));
    }

    if (entity.getUuid() == null) {
      entity.setUuid(UUIDHelper.generateUUIDv7());
    }
  }

  @Override
  protected void preDelete(ControlledVocabularyItem entity) {
    throw new UnsupportedOperationException("DELETE");
  }

  @Override
  public void validateBusinessRules(T entity) {
    applyBusinessRule(entity, itemValidator);
  }

  /**
   * Retrieves vocabulary item matching both a specific key and controlled vocabulary UUID.
   *
   * @param key The key value to match
   * @param controlledVocabularyUuid The UUID of the associated ControlledVocabulary
   * @return The matching item, null if not found
   * @throws IllegalStateException if key and controlledVocabularyUuid returns more than one result
   */
  public T findOneByKey(String key, UUID controlledVocabularyUuid) {
    return findOneByKey(key, controlledVocabularyUuid, null);
  }

  /**
   * Retrieves vocabulary item matching both a specific key and controlled vocabulary UUID.
   *
   * @param key The key value to match
   * @param controlledVocabularyUuid The UUID of the associated ControlledVocabulary
   * @param dinaComponent dinaComponent or null if not used for the controlled vocabulary
   * @return The matching item, null if not found
   * @throws IllegalStateException if key and controlledVocabularyUuid returns more than one result
   */
  public T findOneByKey(String key, UUID controlledVocabularyUuid, String dinaComponent) {
    List<T> results = findAll(
      clazz,
      (criteriaBuilder, root, em) -> {
        Join<T, ControlledVocabulary> vocabularyJoin = root.join("controlledVocabulary", JoinType.INNER);

        List<Predicate> predicates = new ArrayList<>();
        predicates.add(criteriaBuilder.equal(root.get(ControlledVocabularyItem.KEY_ATTRIBUTE_NAME), key));
        predicates.add(criteriaBuilder.equal(vocabularyJoin.get("uuid"), controlledVocabularyUuid));

        if(dinaComponent != null) {
          predicates.add(criteriaBuilder.equal(root.get(ControlledVocabularyItem.DINA_COMPONENT_NAME), dinaComponent));
        }
        return predicates.toArray(new Predicate[0]);
      },
      null, 0, 2, Set.of(), Set.of());

    if (results.size() > 1) {
      throw new IllegalStateException(
        "ControlledVocabularyItem key expected to be unique per ControlledVocabulary: " + key);
    }

    if (results.isEmpty()) {
      return null;
    }

    return results.getFirst();
  }
}
