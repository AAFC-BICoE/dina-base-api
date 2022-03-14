package ca.gc.aafc.dina.extension;

import java.util.List;
import java.util.Objects;

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

    public boolean matchesKeyVersion(String key, String version) {
      return Objects.equals(this.key, key) && Objects.equals(this.version, version);
    }

    public boolean containsTerm(String term) {
      return getFieldByTerm(term) != null;
    }

    public Field getFieldByTerm(String term) {
      if (CollectionUtils.isEmpty(fields)) {
        return null;
      }

      for (Field field : fields) {
        if (field.termEquals(term)) {
          return field;
        }
      }
      return null;
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
    private String[] acceptedValues;
    private String dinaComponent;

    /**
     * Checks if the provided value is in the acceptedValues
     * If acceptedValues or value is null this method will return false;
     */
    public boolean isAcceptedValues(String value) {
      if (acceptedValues == null || StringUtils.isBlank(value)) {
        return false;
      }

      for (String currVal : acceptedValues ) {
        if (currVal.equals(value)) {
          return true;
        }
      }
      return false;
    }

    public boolean termEquals(String term) {
      return StringUtils.equals(this.term, term);
    }
  }
  
}
