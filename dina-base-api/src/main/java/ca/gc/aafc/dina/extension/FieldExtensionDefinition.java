package ca.gc.aafc.dina.extension;

import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class FieldExtensionDefinition {
  
  private Extension extension;
 
  @Builder
  @Getter
  @Setter
  @AllArgsConstructor
  @NoArgsConstructor
  public static class Extension {
  
    private String name;
    private String key;
    private String version;
    private List<Field> fields = new ArrayList<>();
  }
  
  @Builder
  @Getter
  @Setter
  @AllArgsConstructor
  @NoArgsConstructor
  public static class Field {
  
    private String term;
    private String name;
    private String definition;
    private String dinaComponent;
  }

  
}
