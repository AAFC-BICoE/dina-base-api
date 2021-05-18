package ca.gc.aafc.dina.service;

import ca.gc.aafc.dina.entity.ManagedAttribute;
import ca.gc.aafc.dina.jpa.BaseDAO;
import org.springframework.stereotype.Component;


@Component("managedAttributeService")
public class DefaultManagedAttributeService<T extends ManagedAttribute> extends ManagedAttributeService<T> {

  public DefaultManagedAttributeService(BaseDAO baseDAO) {
    super(baseDAO);
  }
  
}
