package ca.gc.aafc.dina.repository.meta;

import ca.gc.aafc.dina.mapper.IgnoreDinaMapping;
import io.crnk.core.resource.annotations.JsonApiMetaInformation;
import io.crnk.core.resource.meta.MetaInformation;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

public abstract class WarningInfoProvider {

  @JsonApiMetaInformation
  @IgnoreDinaMapping
  @Getter
  @Setter
  private WarningMetaInfo meta;

  @Data
  @Builder
  public static class WarningMetaInfo implements MetaInformation {
    private String key;
    private String value;
  }

}
