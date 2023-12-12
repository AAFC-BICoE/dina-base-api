package ca.gc.aafc.dina.file;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.TemporalUnit;
import java.util.EnumSet;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import lombok.SneakyThrows;

/**
 * Deletes file, recursively, from a root path if the provided predicate returns true.
 * By default, only files will be checked (no folders/no symlinks).
 *
 * Some checks are also included to avoid file system root or non-existing folder.
 */
public final class FileCleaner {

  public enum Options { ALLOW_NON_TMP }

  private static final String TMP_DIR_PROPERTY = "java.io.tmpdir";

  private final Path rootPath;
  private final Predicate<Path> predicate;

  /**
   * Creates a default instance where the provided predicate will be combined with the buildFileOnlyPredicate.
   * Folders and symlinks will be ignored.
   * @param rootPath
   * @param predicate
   * @return
   */
  public static FileCleaner newInstance(Path rootPath, Predicate<Path> predicate) {
    return new FileCleaner(rootPath, buildFileOnlyPredicate().and(predicate), null);
  }

  /**
   * Creates an instance with specific options.
   * Use carefully, options gives more flexibility but requires the caller to do more checks to avoid
   * unwanted destructive (file delete) operations.
   * @param rootPath
   * @param predicate
   * @param options
   * @return
   */
  public static FileCleaner newInstance(Path rootPath, Predicate<Path> predicate, EnumSet<Options> options) {
    Objects.requireNonNull(options);
    return new FileCleaner(rootPath, buildFileOnlyPredicate().and(predicate), options);
  }

  /**
   * Private constructor to avoid misuse of always true predicate.
   * @param rootPath
   * @param predicate
   */
  private FileCleaner(Path rootPath, Predicate<Path> predicate, EnumSet<Options> options) {
    // sanity checks
    Objects.requireNonNull(rootPath);
    Objects.requireNonNull(predicate);

    Path normalizedRootPath = rootPath.normalize();
    if (!normalizedRootPath.toFile().isDirectory() || !normalizedRootPath.toFile().exists()) {
      throw new IllegalArgumentException(
        "FileCleaner can only be initialized on an existing directory");
    }

    // by default (no options provided) we restrict to tmp directory
    boolean restrictToTmpDirectory = options == null || !options.contains(Options.ALLOW_NON_TMP);

    if (restrictToTmpDirectory && !normalizedRootPath.startsWith(System.getProperty(TMP_DIR_PROPERTY))) {
      throw new IllegalArgumentException(
        "FileCleaner can only be initialized on a directory under " +
          System.getProperty(TMP_DIR_PROPERTY));
    }

    if (StreamSupport.stream(normalizedRootPath.getFileSystem().getRootDirectories().spliterator(), false)
      .anyMatch(p -> p.equals(normalizedRootPath))) {
      throw new IllegalArgumentException("can't initialize FileCleaner on a root directory");
    }

    this.rootPath = normalizedRootPath;
    this.predicate = predicate;
  }

  /**
   * Build a predicate that is checking for the maximum age of a file based on its lastModifiedTime.
   * @param unit
   * @param maxAge
   * @return
   */
  public static Predicate<Path> buildMaxAgePredicate(TemporalUnit unit, long maxAge) {
    return path -> {
      Duration interval = Duration.between(getLastModifiedTime(path).toInstant(), Instant.now());
      return interval.get(unit) > maxAge;
    };
  }

  /**
   * Build a predicate for checking for a specific file extension of a file.
   * 
   * The check for the extension is not case-sensitive. 
   * 
   * @param fileExtension the extension to check for, with or without the
   *                      beginning dot. e.g: ".txt" or "txt" are accepted.
   * @return predicate checking the extension based on the file extension
   *         provided.
   */
  public static Predicate<Path> buildFileExtensionPredicate(String fileExtension) {
    return path -> path.getFileName().toString().toLowerCase()
        .endsWith(fileExtension.startsWith(".") ? fileExtension.toLowerCase() : "." + fileExtension.toLowerCase());
  }

  /**
   * Excludes folder and symlinks
   * @return
   */
  public static Predicate<Path> buildFileOnlyPredicate() {
    return path -> Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS);
  }

  /**
   * Clean folder recursively by deleting all files that are matching the predicate.
   * @throws IOException
   */
  public void clean() throws IOException {
    try (Stream<Path> p = Files.walk(rootPath)) {
      p.filter(predicate)
        .forEach(FileCleaner::delete);
    }
  }

  @SneakyThrows
  public static FileTime getLastModifiedTime(Path path) {
    return Files.getLastModifiedTime(path);
  }

  @SneakyThrows
  public static void delete(Path path) {
    Files.delete(path);
  }

}
