package ca.gc.aafc.dina.json;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Utility class used to inspect json document.
 *
 */
public final class JsonDocumentInspector {

  private JsonDocumentInspector() {}

  /**
   * Apply the predicate on all values of a json document and return as soon as the predicate returns false.
   * Note: the predicate will not be called on nulls
   *
   * @param jsonElements json elements as returned by Jackson. Map of key values where the value can be an element, a list or a map.
   * @param predicate predicate representing the expected state of the value. When the predicate returns false
   *                  the inspection of the document stops and return.
   * @return
   */
  public static boolean testPredicateOnValues(Map<String, Object> jsonElements, Predicate<String> predicate) {

    for (Map.Entry<String, ?> entry : jsonElements.entrySet()) {
      if (entry.getValue() instanceof Map) {
        if (!testPredicateOnValues((Map<String, Object>) entry.getValue(), predicate)) {
          return false;
        }
      } else if (entry.getValue() instanceof List<?> list) {
        for (Object o : list) {
          if (o instanceof Map) {
            if (!testPredicateOnValues((Map<String, Object>) o, predicate)) {
              return false;
            }
          }
        }
      } else {
        if (entry.getValue() != null) {
          if (!predicate.test(entry.getValue().toString())) {
            return false;
          }
        }
      }
    }
    return true;
  }

}
