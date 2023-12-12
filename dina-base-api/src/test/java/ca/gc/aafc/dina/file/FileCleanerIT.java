package ca.gc.aafc.dina.file;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
        FileCleaner.buildFileExtensionPredicate(EXTENSION_TXT));
    ttc.clean();

    // ".txt" file should be deleted. ".md" should exist. "txt" without the dot should not be deleted.
    assertFalse(txtFile.toFile().exists());
    assertTrue(mdFile.toFile().exists());
    assertTrue(txtNoDotFile.toFile().exists());
  }
}
