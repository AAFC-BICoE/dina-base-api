package ca.gc.aafc.dina.repository.meta;

import ca.gc.aafc.dina.mapper.IgnoreDinaMapping;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import io.crnk.core.resource.annotations.JsonApiMetaInformation;
import io.crnk.core.resource.meta.MetaInformation;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

/**
 * Abstract class which can be extended by a resource to add meta information to a resource's JSON
 * response through Crnk.
 */
public abstract class AttributeMetaInfoProvider {

  @JsonApiMetaInformation
  @IgnoreDinaMapping
  @Getter
  @Setter
  private DinaJsonMetaInfo meta;

  @Builder
  public static class DinaJsonMetaInfo implements MetaInformation {
    private Map<String, Object> properties;
    private Map<String, Object> warnings;

    public void setProperties(Map<String, Object> properties) {
      this.properties = properties;
    }

    public void setWarnings(Map<String, Object> warnings) {
      this.warnings = warnings;
    }

    public Map<String, Object> getProperties() {
      return properties;
    }

    public void setProperties(String propertyName, Object propertyValue) {
      if (properties == null) {
        properties = new HashMap<>();
      }
      this.properties.put(propertyName, propertyValue);
    }

    public Map<String, Object> getWarnings() {
      return warnings;
    }

    public void setWarnings(String warningName, Object warningValue) {
      if (warnings == null) {
        warnings = new HashMap<>();
      }
      this.warnings.put(warningName, warningValue);
    }
  }

}
