package ca.gc.aafc.dina.jsonapi;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.TypeRef;
import com.jayway.jsonpath.spi.json.JacksonJsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;
import com.jayway.jsonpath.Configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class JsonPathHelperTest {

  private static final Configuration JSON_PATH_CONF =  Configuration.builder()
    .mappingProvider(new JacksonMappingProvider())
    .jsonProvider(new JacksonJsonProvider())
    .build();

  @Test
  public void extractById_onList_rightDocumentSectionReturned() {
    String document = """
      [{"id":"A","attribute1":"val1"},{"id":"B","attribute1":"val2"},{"id":"C","attribute1":"val3"}]
      """;
    DocumentContext dc = JsonPath.using(JSON_PATH_CONF).parse(document);
    TypeRef<List<Map<String, Object>>> typeRef = new TypeRef<>() {
      };

    List<Map<String, Object>> result = JsonPathHelper.extractById(dc, "B", typeRef);
    assertNotNull(result);
    assertEquals(1, result.size());
    assertEquals("val2", ((Map<?,?>)result.get(0)).get("attribute1"));
  }
}
