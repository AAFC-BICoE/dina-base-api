package ca.gc.aafc.dina.service;

import org.apache.commons.lang3.StringUtils;
import org.springframework.validation.SmartValidator;

import ca.gc.aafc.dina.entity.IdentifierType;
import ca.gc.aafc.dina.jpa.BaseDAO;
import ca.gc.aafc.dina.validation.IdentifierTypeValidator;
import ca.gc.aafc.dina.vocabulary.VocabularyKeyHelper;

import lombok.NonNull;

/**
 * Service to handle {@link IdentifierType}
 * @param <T>
 */
public class IdentifierTypeService<T extends IdentifierType> extends DefaultDinaService<T> {

  private final IdentifierTypeValidator identifierTypeValidator;

  public IdentifierTypeService(@NonNull BaseDAO baseDAO,
                               @NonNull SmartValidator validator,
                               IdentifierTypeValidator identifierTypeValidator) {
    super(baseDAO, validator);
    this.identifierTypeValidator = identifierTypeValidator;
  }

  @Override
  protected void preCreate(T entity) {
    if (StringUtils.isNotBlank(entity.getName())) {
      entity.setKey(VocabularyKeyHelper.generateKeyFromName(entity.getName()));
    }
  }

  @Override
  public void validateBusinessRules(T identifierType) {
    applyBusinessRule(identifierType, identifierTypeValidator);
  }
}
