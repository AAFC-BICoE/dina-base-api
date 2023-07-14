package ca.gc.aafc.dina.jsonapi;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.PathNotFoundException;
import com.jayway.jsonpath.TypeRef;

public final class JsonPathHelper {

  private JsonPathHelper() {
  }

  /**
   * Extract part of the document by id using JsonPath.
   * @param dc
   * @param id
   * @param typeRef
   * @return
   */
  public static <T> T extractById(DocumentContext dc, String id, TypeRef<T> typeRef) {
    try {
      return dc.read(String.format("$.*[?(@.id == '%s')]", id), typeRef);
    } catch (PathNotFoundException pnf) {
      return null;
    }
  }
}
