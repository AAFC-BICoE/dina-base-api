package ca.gc.aafc.dina.repository.meta;

import ca.gc.aafc.dina.mapper.IgnoreDinaMapping;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.crnk.core.resource.annotations.JsonApiMetaInformation;
import io.crnk.core.resource.meta.MetaInformation;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Abstract class which can be extended by a resource to add meta information to a resource's JSON response
 * through Crnk.
 */
public abstract class AttributeMetaInfoProvider {

  @JsonApiMetaInformation
  @IgnoreDinaMapping
  @Getter
  @Setter
  private DinaJsonMetaInfo meta;

  @Builder
  @Getter
  @Setter
  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  public static class DinaJsonMetaInfo implements MetaInformation {

    private String permissionsProvider;
    private Set<String> permissions;
    private Map<String, Object> warnings;

    public void setWarning(String warningKey, Object warningValue) {
      if (warnings == null) {
        warnings = Collections.emptyMap();
      }
      this.warnings.put(warningKey, warningValue);
    }
  }

  public abstract UUID getUuid();

}
