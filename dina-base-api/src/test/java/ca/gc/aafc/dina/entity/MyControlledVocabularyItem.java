package ca.gc.aafc.dina.entity;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity(name = "test_controlled_vocabulary_item")
@SuperBuilder
@Getter
@Setter
public class MyControlledVocabularyItem extends ControlledVocabularyItem {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = TYPE_COLUMN_NAME)
  private MyControlledVocabulary controlledVocabulary;

}
