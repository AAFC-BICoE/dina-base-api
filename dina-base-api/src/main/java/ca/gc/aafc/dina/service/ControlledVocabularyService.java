package ca.gc.aafc.dina.service;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.validation.SmartValidator;

import ca.gc.aafc.dina.entity.ControlledVocabulary;
import ca.gc.aafc.dina.jpa.BaseDAO;
import ca.gc.aafc.dina.util.UUIDHelper;
import ca.gc.aafc.dina.vocabulary.VocabularyKeyHelper;

import java.util.List;
import lombok.NonNull;

/**
 * Base ControlledVocabulary Service that takes care of the generation of the key.
 *
 * @param <T>
 */
public abstract class ControlledVocabularyService<T extends ControlledVocabulary> extends DefaultDinaService<T> {

  private final Class<T> clazz;

  public ControlledVocabularyService(BaseDAO baseDAO, SmartValidator smartValidator,
                                     @NonNull Class<T> clazz) {
    super(baseDAO, smartValidator);
    this.clazz = clazz;
  }

  @Override
  protected void preCreate(ControlledVocabulary entity) {
    if (StringUtils.isNotBlank(entity.getName())) {
      entity.setKey(VocabularyKeyHelper.generateKeyFromName(entity.getName()));
    }

    if (entity.getUuid() == null) {
      entity.setUuid(UUIDHelper.generateUUIDv7());
    }
  }

  @Override
  protected void preDelete(ControlledVocabulary entity) {
    throw new UnsupportedOperationException("DELETE");
  }

  public T findOneByKey(String key) {
    return findOneByProperty(clazz, ControlledVocabulary.KEY_ATTRIBUTE_NAME, key);
  }

  /**
   * Find a managed attribute by key and another property that is part of the unique constraint.
   * @param key
   * @param andClause clause that should be added to
   * @return
   */
  public T findOneByKeyAnd(String key, Pair<String, Object> andClause) {
    return findOneByProperties(clazz, List.of(Pair.of(ControlledVocabulary.KEY_ATTRIBUTE_NAME, key), andClause));
  }
}
