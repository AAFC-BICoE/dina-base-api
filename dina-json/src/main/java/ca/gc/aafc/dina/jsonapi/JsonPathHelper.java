package ca.gc.aafc.dina.jsonapi;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.PathNotFoundException;
import com.jayway.jsonpath.TypeRef;

/**
 * Utility methods to use jsonPath on documents.
 */
public final class JsonPathHelper {

  private static final String MATCH_ID_JSON_PATH = "$.*[?(@.id == '%s')]";

  // Utility class
  private JsonPathHelper() {
  }

  /**
   * Extracts part of the document by id using JsonPath. Mostly useful to extract a part of document from an array.
   * @param dc documentContext created over the target document
   * @param id the value of id to search on
   * @param typeRef the jsonpath TypeRef
   * @return
   */
  public static <T> T extractById(DocumentContext dc, String id, TypeRef<T> typeRef) {
    try {
      return dc.read(String.format(MATCH_ID_JSON_PATH, id), typeRef);
    } catch (PathNotFoundException pnf) {
      return null;
    }
  }

}
