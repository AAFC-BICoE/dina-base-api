package ca.gc.aafc.dina.file;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.TemporalUnit;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import lombok.SneakyThrows;

/**
 * Deletes file, recursively, from a root path if the provided predicate returns true.
 * By default, only files will be checked (no folders and no symlinks).
 *
 * Some checks are also included to avoid file system root or non-existing folder.
 */
public class TempFileCleaner {

  private final Path rootPath;
  private final Predicate<Path> predicate;

  /**
   * Creates a default instance where the provided predicate will be combined with the buildFileOnlyPredicate.
   * Folders and symlinks will be ignored.
   * @param _rootPath
   * @param predicate
   * @return
   */
  public static TempFileCleaner newInstance(Path _rootPath, Predicate<Path> predicate) {
    return new TempFileCleaner(_rootPath, buildFileOnlyPredicate().and(predicate));
  }

  /**
   * Private constructor to avoid misuse of always true predicate.
   * @param _rootPath
   * @param predicate
   */
  private TempFileCleaner(Path _rootPath, Predicate<Path> predicate) {
    // sanity checks
    Objects.requireNonNull(_rootPath);
    Objects.requireNonNull(predicate);
    Path rootPath = _rootPath.normalize();

    if (StreamSupport.stream(rootPath.getFileSystem().getRootDirectories().spliterator(), false)
      .anyMatch(p -> p.equals(rootPath))) {
      throw new IllegalArgumentException("can't initialize TempFileCleaner on a root directory");
    }

    if (!rootPath.toFile().exists()) {
      throw new IllegalArgumentException(
        "can't initialize TempFileCleaner on a non-existing directory");
    }

    this.rootPath = rootPath;
    this.predicate = predicate;
  }

  public static Predicate<Path> buildMaxAgePredicate(TemporalUnit unit, long maxAge) {
    return (path) -> {
      Duration interval = Duration.between(getLastModifiedTime(path).toInstant(), Instant.now());
      return interval.get(unit) > maxAge;
    };
  }

  /**
   * Excludes folder and symlinks
   * @return
   */
  public static Predicate<Path> buildFileOnlyPredicate() {
    return (path) -> Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS);
  }

  public void clean() throws IOException {
    try (Stream<Path> p = Files.walk(rootPath)) {
      p.filter(predicate)
        .forEach(TempFileCleaner::delete);
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
