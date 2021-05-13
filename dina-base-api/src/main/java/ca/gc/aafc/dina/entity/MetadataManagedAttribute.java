package ca.gc.aafc.dina.entity;

import java.util.Map;

public interface MetadataManagedAttribute extends DinaEntity {
  
  ManagedAttribute getManagedAttribute();

  Map.Entry<String,String> getMetaData();

  String getAssignedValue();
}
