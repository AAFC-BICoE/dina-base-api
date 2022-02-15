package ca.gc.aafc.dina.extension;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

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
    private List<Field> fields;

    public boolean containsTerm(String term) {
      if (CollectionUtils.isEmpty(fields)) {
        return false;
      }

      for (Field field : fields) {
        if (field.termEquals(term)) {
          return true;
        }
      }
      return false;
    }
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

    public boolean termEquals(String term) {
      return StringUtils.equals(this.term, term);
    }
  }
  
}
