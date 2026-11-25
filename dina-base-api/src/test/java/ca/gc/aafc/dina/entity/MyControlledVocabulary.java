package ca.gc.aafc.dina.entity;

import jakarta.persistence.Entity;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Test implementation of {@link ControlledVocabulary}
 */

@Entity(name = "test_controlled_vocabulary")
@SuperBuilder
@NoArgsConstructor
public class MyControlledVocabulary extends ControlledVocabulary {

}
