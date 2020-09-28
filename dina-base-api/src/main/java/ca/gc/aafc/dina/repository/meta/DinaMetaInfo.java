package ca.gc.aafc.dina.repository.meta;

import io.crnk.core.resource.meta.DefaultPagedMetaInformation;

import java.util.Map;

public class DinaMetaInfo extends DefaultPagedMetaInformation {

  private Map<String, String> externalTypes;

  public Map<String, String> getExternalTypes() {
    return externalTypes;
  }

  public void setExternalTypes(Map<String, String> externalTypes) {
    this.externalTypes = externalTypes;
  }
}
