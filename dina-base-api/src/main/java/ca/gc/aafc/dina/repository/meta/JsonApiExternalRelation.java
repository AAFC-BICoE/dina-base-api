package ca.gc.aafc.dina.repository.meta;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface JsonApiExternalRelation {
  String type();
}
