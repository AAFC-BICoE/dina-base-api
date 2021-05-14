package ca.gc.aafc.dina.entity;

import java.util.Map;

public interface ManagedAttributeValue extends DinaEntity {
  
  ManagedAttribute getManagedAttribute();

  Map.Entry<String,String> getMetadata();
}
