package ca.gc.aafc.dina.testsupport.factories;

import java.util.List;

import ca.gc.aafc.dina.i18n.MultilingualDescription;

public class MultilingualDescriptionFactory implements TestableEntityFactory<MultilingualDescription> {

  @Override
  public MultilingualDescription getEntityInstance() {
    return newMultilingualDescription().build();
  }
  
  /**
   * Static method that can be called to return a configured builder that can be further customized
   * to return the actual entity object, call the .build() method on a builder.
   * 
   * @return Pre-configured builder with all mandatory fields set
   */
  public static MultilingualDescription.MultilingualDescriptionBuilder newMultilingualDescription() {
    return MultilingualDescription.builder()
            .descriptions(List.of(
              MultilingualDescription.MultilingualPair.builder()
                .desc("attrEn")
                .lang("en")
                .build(), 
              MultilingualDescription.MultilingualPair.builder()
                .desc("attrFr")
                .lang("fr")
                .build()
            ));
   } 
  
}
