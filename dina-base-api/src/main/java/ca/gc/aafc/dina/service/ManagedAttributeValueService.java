package ca.gc.aafc.dina.service;

import javax.inject.Inject;

import ca.gc.aafc.dina.entity.ManagedAttributeValue;
import ca.gc.aafc.dina.jpa.BaseDAO;
import ca.gc.aafc.dina.validation.ManagedAttributeValueValidator;

public abstract class ManagedAttributeValueService<T extends ManagedAttributeValue> 
  extends DefaultDinaService<T> {

    @Inject
    private ManagedAttributeValueValidator metadataManagedAttributeValidator;
  
    public ManagedAttributeValueService(BaseDAO baseDAO) {
      super(baseDAO);
    }

    @Override
    protected void preCreate(T entity) {
      validateBusinessRules(entity, metadataManagedAttributeValidator);
    }

}
