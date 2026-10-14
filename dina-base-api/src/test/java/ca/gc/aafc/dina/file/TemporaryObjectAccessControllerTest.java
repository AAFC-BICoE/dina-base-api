package ca.gc.aafc.dina.file;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TemporaryObjectAccessControllerTest {

  private final TestController controller = new TestController(Set.of("CSV"));

  @Test
  void generatePath_rejectsAbsoluteAndTraversalPaths() {
    assertThrows(IllegalArgumentException.class,
      () -> controller.generatePath("../outside.csv"));
    assertThrows(IllegalArgumentException.class,
      () -> controller.generatePath(Path.of("/tmp", "outside.csv").toString()));
  }

  @Test
  void registerObject_rejectsMissingAndUnsupportedFiles() throws Exception {
    assertThrows(IllegalArgumentException.class,
      () -> controller.registerObject("missing.csv"));

    String filename = "unsupported-" + UUID.randomUUID() + ".zip";
    Path file = controller.generatePath(filename);
    try {
      Files.writeString(file, "test");
      assertThrows(IllegalArgumentException.class, () -> controller.registerObject(filename));
    } finally {
      Files.deleteIfExists(file);
    }
  }

  @Test
  void downloadObject_servesConfiguredExtensionOnce() throws Exception {
    String filename = "download-" + UUID.randomUUID() + ".CsV";
    String contents = "download contents";
    Path file = controller.generatePath(filename);
    try {
      Files.writeString(file, contents);
      String key = controller.registerObject(filename);

      ResponseEntity<InputStreamResource> response = controller.downloadObject(key);
      assertEquals(HttpStatus.OK, response.getStatusCode());
      assertEquals(MediaType.APPLICATION_OCTET_STREAM, response.getHeaders().getContentType());
      assertTrue(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION).contains(filename));
      assertEquals(contents.length(), response.getHeaders().getContentLength());

      InputStreamResource body = response.getBody();
      assertNotNull(body);
      try (InputStream input = body.getInputStream()) {
        assertEquals(contents, new String(input.readAllBytes(), StandardCharsets.UTF_8));
      }

      assertEquals(HttpStatus.NOT_FOUND, controller.downloadObject(key).getStatusCode());
    } finally {
      Files.deleteIfExists(file);
    }
  }

  private static final class TestController extends TemporaryObjectAccessController {
    private TestController(Set<String> extensions) {
      super(extensions);
    }
  }
}
