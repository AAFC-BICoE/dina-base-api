package ca.gc.aafc.dina.file;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FileCleanerIT {

  @Test
  public void tempFileCleaner_onAlwaysTruePredicate_fileRemoved() throws IOException {

    Path testFolder = Files.createTempDirectory("dina-test");

    final String testText = "this is a test";
    final String testFilename = UUID.randomUUID() + ".txt";
    final String testFilename2 = UUID.randomUUID() + ".txt";

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
}
