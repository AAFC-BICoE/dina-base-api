package ca.gc.aafc.dina.entity;

import javax.persistence.Entity;
import lombok.experimental.SuperBuilder;

/**
 * Test implementation of {@link ControlledVocabulary}
 */

@Entity(name = "test_controlled_vocabulary")
@SuperBuilder
public class MyControlledVocabulary extends ControlledVocabulary {

}
