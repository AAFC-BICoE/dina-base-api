package ca.gc.aafc.dina.metaInfo;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface JsonApiExternalRelation {
  String type();
}
