package ca.gc.aafc.dina.file;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FileCleanerIT {

  private static final String EXTENSION_TXT = "txt";
  private static final String EXTENSION_MD = "md";

  @Test
  public void fileCleaner_onAlwaysTruePredicate_fileRemoved() throws IOException {

    Path testFolder = Files.createTempDirectory("dina-test");

    final String testText = "this is a test";
    final String testFilename = UUID.randomUUID() + "." + EXTENSION_TXT;
    final String testFilename2 = UUID.randomUUID() + "." + EXTENSION_TXT;

    Path p = testFolder.resolve(testFilename);
    Files.writeString(p, testText);

    FileCleaner ttc = FileCleaner.newInstance(testFolder, (path) -> true);
    ttc.clean();
    assertFalse(p.toFile().exists());

    Path innerFolder = testFolder.resolve("folder1");
    assertTrue(innerFolder.toFile().mkdir());
    Path p2 = innerFolder.resolve(testFilename2);
    Files.writeString(p2, testText);

    ttc = FileCleaner.newInstance(testFolder, (path) -> true);
    ttc.clean();
    assertFalse(p2.toFile().exists());

    // default instance will not remove folders
    assertTrue(innerFolder.toFile().exists());
  }

  @Test
  public void fileCleaner_predicateOnFileExtension_specificFilesRemoved() throws IOException {
    Path testFolder = Files.createTempDirectory("dina-test");

    final String testText = "this is a test";
    final String txtFilename = UUID.randomUUID() + "." + EXTENSION_TXT;
    final String mdFilename = UUID.randomUUID() + "." + EXTENSION_MD;
    final String txtNoDotFilename = UUID.randomUUID() + EXTENSION_TXT; // Without "."

    Path txtFile = testFolder.resolve(txtFilename);
    Path mdFile = testFolder.resolve(mdFilename);
    Path txtNoDotFile = testFolder.resolve(txtNoDotFilename);
    Files.writeString(txtFile, testText);
    Files.writeString(mdFile, testText);
    Files.writeString(txtNoDotFile, testText);

    FileCleaner ttc = FileCleaner.newInstance(testFolder,
        FileCleaner.buildFileExtensionPredicate(EXTENSION_TXT.toUpperCase()));
    ttc.clean();

    // ".txt" file should be deleted. ".md" should exist. "txt" without the dot should not be deleted.
    assertFalse(txtFile.toFile().exists());
    assertTrue(mdFile.toFile().exists());
    assertTrue(txtNoDotFile.toFile().exists());
  }

  @Test
  public void fileCleaner_predicateOnMaxAge_oldFilesRemoved() throws IOException, InterruptedException {
    Path testFolder = Files.createTempDirectory("dina-test");

    final String testText = "this is a test";
    final String oldFilename = UUID.randomUUID() + "." + EXTENSION_TXT;
    final String newFilename = UUID.randomUUID() + "." + EXTENSION_TXT;

    Path oldFile = testFolder.resolve(oldFilename);
    Path newFile = testFolder.resolve(newFilename);
    Files.writeString(oldFile, testText);
    Files.writeString(newFile, testText);

    // Simulate the oldFile being older by an hour.
    Instant now = Instant.now();
    Instant oneHourAgo = now.minus(Duration.ofHours(1));
    FileTime fileTime = FileTime.from(oneHourAgo); 
    Files.setLastModifiedTime(oldFile, fileTime);

    FileCleaner ttc = FileCleaner.newInstance(testFolder,
        FileCleaner.buildMaxAgePredicate(ChronoUnit.SECONDS, 1800));
    ttc.clean();

    // Old file should be deleted. New file should exist.
    assertFalse(oldFile.toFile().exists());
    assertTrue(newFile.toFile().exists());
  }

  @Test
  public void fileCleaner_onNonExistingDirectory_throwsException() {
    Path nonExistingDir = Path.of("/non-existing-dir");

    IllegalArgumentException exception = org.junit.jupiter.api.Assertions.assertThrows(
        IllegalArgumentException.class,
        () -> FileCleaner.newInstance(nonExistingDir, (path) -> true)
    );

    assertTrue(exception.getMessage().contains("FileCleaner can only be initialized on an existing directory"));
  }
}
