package ca.gc.aafc.dina.client.file;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import ca.gc.aafc.dina.client.config.OpenIdConnectConfig;
import mockwebserver3.Dispatcher;
import mockwebserver3.MockResponse;
import mockwebserver3.MockWebServer;
import mockwebserver3.RecordedRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FileDownloaderTest {

  private static final String ACCESS_TOKEN = "test-access-token";
  private static final String FILE_CONTENT = "col1,col2\nval1,val2\n";

  private static final String TOKEN_RESPONSE = """
    {"access_token":"%s","refresh_token":"refresh","expires_in":300,"token_type":"Bearer"}
    """.formatted(ACCESS_TOKEN);

  @TempDir
  Path tempDir;

  private MockWebServer server;
  private FileDownloader fileDownloader;

  @BeforeEach
  void setUp() throws IOException {
    server = new MockWebServer();
    server.start();

    OpenIdConnectConfig config = new OpenIdConnectConfig();
    config.setOpenIdConnectBaseUrl(server.url("/auth/").toString());
    config.setClientId("client");
    config.setUsername("user");
    config.setPassword("password");

    fileDownloader = new FileDownloader(config, Set.of(server.getHostName()));
  }

  @AfterEach
  void tearDown() {
    server.close();
  }

  @Test
  void downloadFile_onSuccess_fileWrittenWithBearerToken() throws Exception {
    server.setDispatcher(dispatcher(new MockResponse.Builder()
      .code(200)
      .setHeader("Content-Disposition", "attachment; filename=\"export.csv\"")
      .body(FILE_CONTENT)
      .build()));

    List<String> receivedFilenames = new ArrayList<>();
    fileDownloader.downloadFile(server.url("/file/abc").toString(), filename -> {
      receivedFilenames.add(filename);
      return tempDir.resolve(filename);
    });

    assertEquals(List.of("export.csv"), receivedFilenames);
    assertEquals(FILE_CONTENT,
      Files.readString(tempDir.resolve("export.csv"), StandardCharsets.UTF_8));

    RecordedRequest downloadRequest = takeDownloadRequest();
    assertEquals("Bearer " + ACCESS_TOKEN, downloadRequest.getHeaders().get("Authorization"));
  }

  @Test
  void downloadFile_onPathTraversalFilename_filenameSanitized() throws Exception {
    server.setDispatcher(dispatcher(new MockResponse.Builder()
      .code(200)
      .setHeader("Content-Disposition", "attachment; filename=\"../../evil.txt\"")
      .body(FILE_CONTENT)
      .build()));

    List<String> receivedFilenames = new ArrayList<>();
    fileDownloader.downloadFile(server.url("/file/abc").toString(), filename -> {
      receivedFilenames.add(filename);
      return tempDir.resolve(filename);
    });

    assertEquals(List.of("evil.txt"), receivedFilenames);
    assertEquals(FILE_CONTENT, Files.readString(tempDir.resolve("evil.txt")));
  }

  @Test
  void downloadFile_onErrorResponse_destinationFunctionNotCalled() {
    server.setDispatcher(dispatcher(new MockResponse.Builder()
      .code(404)
      .setHeader("Content-Disposition", "attachment; filename=\"export.csv\"")
      .build()));

    List<String> receivedFilenames = new ArrayList<>();
    assertThrows(IllegalStateException.class, () ->
      fileDownloader.downloadFile(server.url("/file/abc").toString(), filename -> {
        receivedFilenames.add(filename);
        return tempDir.resolve("export.csv");
      }));

    assertTrue(receivedFilenames.isEmpty());
  }

  @Test
  void downloadFile_onNoContentDisposition_nullFilenameGiven() throws Exception {
    server.setDispatcher(dispatcher(new MockResponse.Builder()
      .code(200)
      .body(FILE_CONTENT)
      .build()));

    List<String> receivedFilenames = new ArrayList<>();
    Path destination = tempDir.resolve("default.csv");
    fileDownloader.downloadFile(server.url("/file/abc").toString(), filename -> {
      receivedFilenames.add(filename);
      return destination;
    });

    assertEquals(1, receivedFilenames.size());
    assertNull(receivedFilenames.getFirst());
    assertEquals(FILE_CONTENT, Files.readString(destination));
  }

  @Test
  void downloadFile_onErrorResponse_exceptionThrownAndExistingFileUnchanged() throws Exception {
    server.setDispatcher(dispatcher(new MockResponse.Builder()
      .code(500)
      .setHeader("Content-Disposition", "attachment; filename=\"export.csv\"")
      .body("server error")
      .build()));

    Path destination = tempDir.resolve("export.csv");
    Files.writeString(destination, "original");

    assertThrows(IllegalStateException.class, () ->
      fileDownloader.downloadFile(server.url("/file/abc").toString(), filename -> destination));

    assertEquals("original", Files.readString(destination));
  }

  @Test
  void downloadFile_onHostNotAllowed_exceptionThrownAndNoRequestSent() {
    server.setDispatcher(dispatcher(new MockResponse.Builder().code(200).build()));

    assertThrows(IllegalArgumentException.class, () ->
      fileDownloader.downloadFile("http://example.org/file/abc", tempDir::resolve));

    assertEquals(0, server.getRequestCount());
  }

  @Test
  void downloadFile_onInvalidUrl_exceptionThrown() {
    assertThrows(IllegalArgumentException.class, () ->
      fileDownloader.downloadFile("not a url", tempDir::resolve));

    assertEquals(0, server.getRequestCount());
  }

  /**
   * Serves the OpenID Connect token endpoint and returns the provided response for any other path.
   */
  private static Dispatcher dispatcher(MockResponse downloadResponse) {
    return new Dispatcher() {
      @Override
      public MockResponse dispatch(RecordedRequest request) {
        if ("/auth/token".equals(request.getUrl().encodedPath())) {
          return new MockResponse.Builder()
            .code(200)
            .setHeader("Content-Type", "application/json")
            .body(TOKEN_RESPONSE)
            .build();
        }
        return downloadResponse;
      }
    };
  }

  private RecordedRequest takeDownloadRequest() throws InterruptedException {
    RecordedRequest request;
    do {
      request = server.takeRequest(1, TimeUnit.SECONDS);
    } while (request != null && request.getUrl().encodedPath().startsWith("/auth/"));
    if (request == null) {
      throw new AssertionError("No download request received");
    }
    return request;
  }
}
