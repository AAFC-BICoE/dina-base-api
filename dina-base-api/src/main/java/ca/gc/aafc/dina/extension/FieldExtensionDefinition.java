package ca.gc.aafc.dina.extension;

import java.util.List;
import java.util.Objects;

import ca.gc.aafc.dina.i18n.MultilingualDescription;
import ca.gc.aafc.dina.i18n.MultilingualTitle;
import ca.gc.aafc.dina.vocabulary.TypedVocabularyElement;
import ca.gc.aafc.dina.vocabulary.VocabularyElement;

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

    public boolean containsKey(String key) {
      return getFieldByKey(key) != null;
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

    public Field getFieldByKey(String key) {
      if (CollectionUtils.isEmpty(fields)) {
        return null;
      }

      for (Field field : fields) {
        if (field.keyEquals(key)) {
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
  public static class Field implements TypedVocabularyElement {

    /**
     * The key should not contain dot(.) since it is the character used to reference
     * a key within an extension. See {@link VocabularyElement#getKey()}
     */
    private String key;

    private String name;

    // usually a URI
    private String term;

    private String unit;

    private VocabularyElementType vocabularyElementType;
    private String[] acceptedValues;

    private MultilingualDescription multilingualDescription;
    private MultilingualTitle multilingualTitle;
    private String dinaComponent;

    private String conceptQueryEndpoint;
    private String topLevelConcept;

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

    public boolean keyEquals(String key) {
      return StringUtils.equals(this.key, key);
    }
  }

}
