package ca.gc.aafc.dina.file;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAmount;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;

import jakarta.annotation.PreDestroy;
import lombok.extern.log4j.Log4j2;

/**
 * Registers temporary files for one-time downloads using expiring access tokens.
 * Files are stored in a dedicated temporary directory and limited to the supported
 * extensions configured by this class or a subclass.
 */
@Log4j2
public class TemporaryObjectAccessController {

  protected static final Set<String> DEFAULT_SUPPORTED_FILE_EXT = Set.of("zip");

  private static final Path WORKING_FOLDER = assignWorkingDir();
  private static final long MAX_AGE_MINUTES = 60;
  private static final TemporalAmount MAX_AGE = Duration.ofMinutes(MAX_AGE_MINUTES);
  private static final int MAX_OUTSTANDING_ACCESS_TOKENS = 1_000;

  private static final ConcurrentHashMap<String, TemporaryObjectAccess> ACCESS_MAP
    = new ConcurrentHashMap<>();

  private final Set<String> supportedFileExt;
  private final FileCleaner fileCleaner;

  public TemporaryObjectAccessController() {
    this(DEFAULT_SUPPORTED_FILE_EXT);
  }

  protected TemporaryObjectAccessController(Set<String> supportedFileExt) {
    if (supportedFileExt == null || supportedFileExt.isEmpty()) {
      throw new IllegalArgumentException("At least one supported file extension is required");
    }
    this.supportedFileExt = supportedFileExt.stream()
      .map(ext -> ext.toLowerCase(Locale.ROOT))
      .collect(Collectors.toUnmodifiableSet());
    fileCleaner = FileCleaner.newInstance(WORKING_FOLDER,
      FileCleaner.buildMaxAgePredicate(ChronoUnit.SECONDS, MAX_AGE_MINUTES * 60)
        .and(buildFileExtensionPredicate(this.supportedFileExt)));
  }

  private static Path assignWorkingDir() {
    try {
      return Files.createTempDirectory("dina_tao_tmp");
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static Predicate<Path> buildFileExtensionPredicate(Set<String> extensions) {

    Optional<String> firstExt = extensions.stream().findFirst();

    if (firstExt.isEmpty()) {
      throw new IllegalStateException("Requires at least 1 extension");
    }

    // create a predicate for the first file extension
    Predicate<Path> fileExtensionPredicate =
      FileCleaner.buildFileExtensionPredicate(firstExt.get());
    Set<String> filteredFileExt =
      extensions.stream().filter(s -> !s.equals(firstExt.get())).collect(
        Collectors.toSet());

    // if there is more than 1, combine with an OR
    for (String ext : filteredFileExt) {
      fileExtensionPredicate =
        fileExtensionPredicate.or(FileCleaner.buildFileExtensionPredicate(ext));
    }
    return fileExtensionPredicate;
  }

  /**
   * Generate a path to store a temporary file that should be available for
   * temporary object access.
   * @param filename
   * @return
   */
  public Path generatePath(String filename) {
    return resolveTemporaryFile(filename);
  }

  /**
   * Resolves a single filename inside the temporary directory. Absolute paths and
   * path components such as {@code ..} are rejected to prevent escaping that directory.
   *
   * @throws IllegalArgumentException if the filename is blank or is not a single filename
   */
  private static Path resolveTemporaryFile(String filename) {
    if (filename == null || filename.isBlank()) {
      throw new IllegalArgumentException("filename must not be blank");
    }

    Path name = Path.of(filename);
    if (name.isAbsolute() || name.getNameCount() != 1
      || !name.toString().equals(filename)
      || ".".equals(filename) || "..".equals(filename)) {
      throw new IllegalArgumentException("filename must be a single file name");
    }

    Path file = WORKING_FOLDER.resolve(name).normalize();
    if (!file.startsWith(WORKING_FOLDER)) {
      throw new IllegalArgumentException("file must be within the temporary directory");
    }
    return file;
  }

  /**
   * Register the file so it becomes available for download (a single time) using the returned key.
   * @param filename
   * @return the single-use TemporaryObjectAccess key that can be used to download the file
   */
  public String registerObject(String filename) {

    // run the clean method on new requests to avoid a scheduled method (at least for now)
    cleanExpiredFile();

    Path file = resolveTemporaryFile(filename);
    if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) {
      throw new IllegalArgumentException("the file must exist in the temporary directory");
    }

    if (!supportedFileExt.contains(FilenameUtils.getExtension(filename).toLowerCase(Locale.ROOT))) {
      throw new IllegalArgumentException("unsupported file extension");
    }

    if (ACCESS_MAP.size() >= MAX_OUTSTANDING_ACCESS_TOKENS) {
      throw new IllegalStateException("Maximum number of outstanding download tokens reached");
    }

    String key = RandomStringUtils.secure().nextAlphanumeric(128);
    TemporaryObjectAccess toa = new TemporaryObjectAccess(filename, Instant.now());
    ACCESS_MAP.put(key, toa);
    return key;
  }

  /**
   * Triggers a download of a file.
   * 
   * Used with @GetMapping("/toa/{key}")
   *
   * @param key the tao key
   * @return a response entity
   */
  protected ResponseEntity<InputStreamResource> downloadObject(@PathVariable String key) throws IOException {
    
    TemporaryObjectAccess toa = ACCESS_MAP.remove(key);

    if (toa == null) {
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    // make sure the toa is not expired
    if (Instant.now().isAfter(toa.createdOn.plus(MAX_AGE))) {
      log.warn("toa expired");
      cleanExpiredFile();
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    Path file = resolveTemporaryFile(toa.filename());
    if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) {
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
    return ResponseEntity.ok()
      .header(HttpHeaders.CONTENT_DISPOSITION,
        ContentDisposition.attachment().filename(toa.filename()).build().toString())
      .contentType(MediaType.APPLICATION_OCTET_STREAM)
      .contentLength(Files.size(file))
      .body(new InputStreamResource(Files.newInputStream(file, LinkOption.NOFOLLOW_LINKS)));
  }

  @PreDestroy
  public void cleanAllFiles() {
    // clean all files since we will lose the keys anyway
    try {
      FileCleaner.newInstance(WORKING_FOLDER,
        buildFileExtensionPredicate(supportedFileExt)).clean();
    } catch (IOException e) {
      log.warn("Unable clear temporary files on exit");
    }
  }

  private void cleanExpiredFile() {
    Instant now = Instant.now();
    ACCESS_MAP.entrySet().removeIf(entry ->
      now.isAfter(entry.getValue().createdOn.plus(MAX_AGE)));

    try {
      fileCleaner.clean();
    } catch (IOException e) {
      log.warn("Unable clear expired files in TemporaryObjectAccessController");
    }
  }

  record TemporaryObjectAccess(String filename, Instant createdOn) {
  }
}
