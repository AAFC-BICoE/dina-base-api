package ca.gc.aafc.dina.client.file;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpHeaders;

import okhttp3.Call;
import okhttp3.Response;
import okhttp3.ResponseBody;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.springframework.http.ContentDisposition;

import ca.gc.aafc.dina.client.AccessTokenAuthenticator;
import ca.gc.aafc.dina.client.TokenBasedRequestBuilder;
import ca.gc.aafc.dina.client.config.OpenIdConnectConfig;
import ca.gc.aafc.dina.client.token.AccessTokenManager;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;

/**
 * Downloads files from trusted DINA endpoints using an OpenID Connect access token.
 */
public class FileDownloader {

  private final OkHttpClient httpClient;
  private final TokenBasedRequestBuilder tokenBasedRequestBuilder;
  private final Set<String> allowedHosts;

  /**
   * @param openIdConnectConfig OpenID Connect configuration used to get the access token
   * @param allowedHosts hosts the access token may be sent to
   */
  public FileDownloader(OpenIdConnectConfig  openIdConnectConfig, Set<String> allowedHosts) {
    AccessTokenManager accessTokenManager = new AccessTokenManager(openIdConnectConfig);
    httpClient = new OkHttpClient.Builder()
      .authenticator(new AccessTokenAuthenticator(accessTokenManager))
      .followRedirects(false)
      .followSslRedirects(false)
      .build();
    tokenBasedRequestBuilder = new TokenBasedRequestBuilder(accessTokenManager);
    this.allowedHosts = allowedHosts;
  }

  /**
   * Downloads a file to the path returned by {@code destinationFileFunction}.
   * The file is written to a temporary file first and moved into place only after
   * the download succeeds.
   *
   * @param downloadUrl url where to download the file from
   * @param destinationFileFunction receives the sanitized filename from the response
   *                                (may be null) and returns the destination path
   * @return the destination path
   */
  public void downloadFile(String downloadUrl, Function<String, Path> destinationFileFunction) throws IOException {
    Objects.requireNonNull(destinationFileFunction, "destinationFileFunction");

    // Prepare url
    HttpUrl parsedDownloadUrl = HttpUrl.parse(downloadUrl);
    if (parsedDownloadUrl == null) {
      throw new IllegalArgumentException("Can't parse downloadUrl: " + downloadUrl);
    }
    if (!allowedHosts.contains(parsedDownloadUrl.host())) {
      throw new IllegalArgumentException("Host not allowed: " + parsedDownloadUrl.host());
    }

    Call downloadCall =
      httpClient.newCall(tokenBasedRequestBuilder.newBuilder().url(parsedDownloadUrl).build());

    try (Response response = downloadCall.execute()) {

      ResponseBody body = response.body();
      if (!response.isSuccessful() || body == null) {
        throw new IllegalStateException("Can't read response body from downloadUrl. Returned code: " + response.code());
      }

      Path destinationFile = destinationFileFunction.apply(extractFilenameFromResponse(response));
      try (OutputStream outputStream = new FileOutputStream(destinationFile.toFile());
           InputStream inputStream = body.byteStream()) {
        IOUtils.copy(inputStream, outputStream);
      }
    }
  }

  /**
   * Gets the filename from the Content-Disposition header and removes any path
   * components to prevent path traversal (e.g. "../../etc/passwd").
   */
  private static String extractFilenameFromResponse(Response response) {
    String contentDisposition = response.header(HttpHeaders.CONTENT_DISPOSITION);
    if (contentDisposition == null) {
      return null;
    }

    String filename;
    try {
      filename = ContentDisposition.parse(contentDisposition).getFilename();
    } catch (IllegalArgumentException e) {
      return null;
    }
    if (StringUtils.isBlank(filename)) {
      return null;
    }

    String sanitized = StringUtils.substringAfterLast("/" + filename.replace('\\', '/'), "/");
    if (StringUtils.isBlank(sanitized) || ".".equals(sanitized) || "..".equals(sanitized)) {
      return null;
    }
    return sanitized;
  }

}
