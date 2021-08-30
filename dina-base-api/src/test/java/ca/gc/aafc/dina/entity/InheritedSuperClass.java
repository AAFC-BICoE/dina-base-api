package ca.gc.aafc.dina.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import javax.persistence.MappedSuperclass;

@Getter
@Setter
@MappedSuperclass
@NoArgsConstructor
@SuperBuilder
public abstract class InheritedSuperClass {
  private String Inherited;
}
