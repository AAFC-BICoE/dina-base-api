package ca.gc.aafc.dina.testsupport;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Utility static class to deal with test resources.
 *
 */
public class TestResourceHelper {
  
  private TestResourceHelper(){
    
  }
  
  /**
   * Read the content of a resource in the classpath. The resource encoding is assumed to be UTF8.
   * Newline character is expected to be \n.
   * 
   * @param resourceName
   * @return
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

}
