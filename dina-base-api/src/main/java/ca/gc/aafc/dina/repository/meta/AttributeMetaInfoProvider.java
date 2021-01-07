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

    public void setProperties(Map<String, Object> properties) {
      this.properties = properties;
    }

    @JsonAnyGetter
    public Map<String, Object> getProperties() {
      return properties;
    }

    @JsonAnySetter
    public void setProperties(String propertyName, Object propertyValue) {
      if (properties == null) {
        properties = new HashMap<>();
      }
      this.properties.put(propertyName, propertyValue);
    }
  }

}
