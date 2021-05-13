package ca.gc.aafc.dina.service;

import javax.inject.Inject;

import ca.gc.aafc.dina.entity.MetadataManagedAttribute;
import ca.gc.aafc.dina.jpa.BaseDAO;
import ca.gc.aafc.dina.validation.MetadataManagedAttributeValidator;

public abstract class MetadataManagedAttributeService<T extends MetadataManagedAttribute> 
  extends DefaultDinaService<T> {

    @Inject
    private MetadataManagedAttributeValidator metadataManagedAttributeValidator;
  
    public MetadataManagedAttributeService(BaseDAO baseDAO) {
      super(baseDAO);
    }

    @Override
    protected void preCreate(T entity) {
      validateBusinessRules(entity, metadataManagedAttributeValidator);
    }

}
