package ca.gc.aafc.dina.entity;

import ca.gc.aafc.dina.i18n.MultilingualTitle;
import ca.gc.aafc.dina.vocabulary.VocabularyElement;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Getter
public abstract class IdentifierType implements VocabularyElement, DinaEntity {

  /**
   * The key should not contain dot(.) See {@link VocabularyElement#getKey()}
   */
  @Setter
  private String key;

  private String name;

  // usually a URI
  private String term;

  private MultilingualTitle multilingualTitle;

  /**
   * The component (material-sample, project) where this identifier type is expected to be used.
   */
  private String dinaComponent;

  /**
   * Like wikidata. A URI template where "$1" can be automatically replaced with the value
   * assigned to the identifier.
   */
  private String uriTemplate;

}
