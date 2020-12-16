package ca.gc.aafc.dina.repository.meta;

import ca.gc.aafc.dina.mapper.IgnoreDinaMapping;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import io.crnk.core.resource.annotations.JsonApiMetaInformation;
import io.crnk.core.resource.meta.MetaInformation;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

public abstract class WarningInfoProvider {

  @JsonApiMetaInformation
  @IgnoreDinaMapping
  @Getter
  @Setter
  private DinaJsonMetaInfo meta;

  @Data
  @Builder
  public static class DinaJsonMetaInfo implements MetaInformation {
    private Map<String, String> properties;

    public void setProperties(Map<String, String> properties) {
      this.properties = properties;
    }

    @JsonAnyGetter
    public Map<String, String> getProperties() {
      return properties;
    }

    @JsonAnySetter
    public void setProperties(String propertyName, String propertyValue) {
      if (properties == null) {
        properties = new HashMap<>();
      }
      this.properties.put(propertyName, propertyValue);
    }
  }

}
