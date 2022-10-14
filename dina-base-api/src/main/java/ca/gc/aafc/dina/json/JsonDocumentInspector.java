package ca.gc.aafc.dina.json;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Utility class used to inspect json document.
 *
 */
public final class JsonDocumentInspector {

  private JsonDocumentInspector() {
  }

  /**
   * Apply the predicate on all values of a json document and return as soon as the predicate returns false.
   * Note: the predicate will not be called on nulls
   *
   * @param jsonElements json elements as returned by Jackson. Map of key values where the value can be an element, a list or a map.
   * @param predicate predicate representing the expected state of the value. When the predicate returns false
   *                  the inspection of the document stops and return. The predicate must handle null.
   * @return
   */
  public static boolean testPredicateOnValues(Map<String, Object> jsonElements, Predicate<String> predicate) {

    Objects.requireNonNull(jsonElements);
    Objects.requireNonNull(predicate);

    for (Map.Entry<String, ?> entry : jsonElements.entrySet()) {
      if (!testValue(entry.getValue(), predicate)) {
        return false;
      }
    }
    return true;
  }

  private static boolean testValue(Object value, Predicate<String> predicate) {
    // if we have a map, send it back to the main method
    if (value instanceof Map) {
      return testPredicateOnValues((Map<String, Object>) value, predicate);
    } else if (value instanceof List<?> list) { // if we have a list, iterate and send it back to this method for each values
      for (Object o : list) {
        if (!testValue(o, predicate)) {
          return false;
        }
      }
    } else { // if we do not have a map or a list then we have a simple value
      return predicate.test(value == null ? null : value.toString());
    }
    return true;
  }

}
