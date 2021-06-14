package ca.gc.aafc.dina.i18n;

import java.util.List;
import java.util.Map;

import lombok.Builder;
import lombok.Getter;

@Builder
public class MultilingualDescription {

  @Getter
  private List<Map<String,String>> multilingualPair;
  
}
