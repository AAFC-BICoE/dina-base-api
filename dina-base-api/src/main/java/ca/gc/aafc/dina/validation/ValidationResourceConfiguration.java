package ca.gc.aafc.dina.validation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.context.annotation.Configuration;

@Configuration
public class ValidationResourceConfiguration {
  
  public static Map<String, Set<String>> parseObjectPerType(List<Object> objects) {
    Map<String, Set<String>> map = new HashMap<>();
    return map;
  }

}
