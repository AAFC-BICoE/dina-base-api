package ca.gc.aafc.dina.i18n;

import java.util.List;

import org.javers.core.metamodel.annotation.Value;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Builder
@Setter
@Getter
@AllArgsConstructor
@RequiredArgsConstructor
@Value
public class MultilingualDescription {

  private List<MultilingualPair> descriptions;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @Value
  public static class MultilingualPair {
    private String lang;

    private String desc;
  }

  
}
