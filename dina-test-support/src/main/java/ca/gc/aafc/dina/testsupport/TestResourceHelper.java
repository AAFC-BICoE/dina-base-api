package ca.gc.aafc.dina.testsupport;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Utility static class to deal with test resources.
 *
 */
@SuppressFBWarnings("RCN_REDUNDANT_NULLCHECK_WOULD_HAVE_BEEN_A_NPE")
public final class TestResourceHelper {

  public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  
  private TestResourceHelper() {
  }
  
  /**
   * Read the content of a resource in the classpath. The resource encoding is assumed to be UTF8.
   * Newline character is expected to be {@literal \n}
   * 
   * @param resourceName
   * @return the content of the resource as String
   * @throws IOException resource not found or invalid
   */
  public static String readContentAsString(String resourceName) throws IOException {
    String resourceContent = "";
    try {

      URL url = TestResourceHelper.class.getClassLoader().getResource(resourceName);

      if (url == null) {
        throw new IOException("Resource " + resourceName + " not found in classpath");
      }
      Path path = Paths.get(url.toURI());

      try (Stream<String> lines = Files.lines(path)) {
        resourceContent = lines.collect(Collectors.joining("\n"));
      }

    } catch (URISyntaxException e) {
      throw new IOException(e);
    }
    return resourceContent;
  }

  /**
   * Read the content of a resource in the classpath.
   * Content is transformed into {@link JsonNode}.
   * @param resourceName
   * @return {@link JsonNode} from the resource
   * @throws IOException
   */
  public static JsonNode readContentAsJsonNode(String resourceName) throws IOException {
    URL url = TestResourceHelper.class.getClassLoader().getResource(resourceName);
    if (url == null) {
      throw new IOException("Resource " + resourceName + " not found in classpath");
    }
    return OBJECT_MAPPER.readTree(url);
  }

}
