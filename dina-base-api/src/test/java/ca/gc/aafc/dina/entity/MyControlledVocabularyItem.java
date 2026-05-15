package ca.gc.aafc.dina.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity(name = "test_controlled_vocabulary_item")
@SuperBuilder
@Getter
@Setter
public class MyControlledVocabularyItem extends ControlledVocabularyItem {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = CONTROLLED_VOCABULARY_COL_NAME)
  private MyControlledVocabulary controlledVocabulary;

}
