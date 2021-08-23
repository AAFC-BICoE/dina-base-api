package ca.gc.aafc.dina.dto;

import ca.gc.aafc.dina.entity.Inherited;
import ca.gc.aafc.dina.entity.InheritedSuperClass;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@RelatedEntity(Inherited.class)
public class InheritedDto extends InheritedSuperClass {

}
