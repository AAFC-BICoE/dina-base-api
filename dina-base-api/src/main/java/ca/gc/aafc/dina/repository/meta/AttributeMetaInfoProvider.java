package ca.gc.aafc.dina.repository.meta;

import ca.gc.aafc.dina.mapper.IgnoreDinaMapping;
import io.crnk.core.resource.annotations.JsonApiMetaInformation;
import io.crnk.core.resource.meta.MetaInformation;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;

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
    private Map<String, Object> warnings;

    public void setWarnings(Map<String, Object> warnings) {
      this.warnings = warnings;
    }

    @JsonAnyGetter
    public Map<String, Object> getWarnings() {
      return warnings;
    }

    @JsonAnySetter
    public void setWarnings(String warningKey, Object warningValue) {
      if (warnings == null) {
        warnings = new HashMap<>();
      }
      this.warnings.put(warningKey, warningValue);
    }
  }

}
