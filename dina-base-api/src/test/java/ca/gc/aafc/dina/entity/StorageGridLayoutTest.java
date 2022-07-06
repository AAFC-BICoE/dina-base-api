package ca.gc.aafc.dina.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class StorageGridLayoutTest {

  @Test
  public void testStorageGridLayout() {

    StorageGridLayout sgl = StorageGridLayout.builder()
            .numberOfRows(5)
            .numberOfColumns(5)
            .fillDirection(StorageGridLayout.FillDirection.BY_ROW)
            .build();

    assertTrue(sgl.isValidLocation(1,2));
    assertTrue(sgl.isValidLocation(5,5));

    // numbers are 1-based
    assertFalse(sgl.isValidLocation(0, 0));

    assertFalse(sgl.isValidLocation(5, 6));

    // StorageGridLayout invalid state
    sgl = StorageGridLayout.builder()
            .numberOfRows(null)
            .numberOfColumns(null)
            .fillDirection(StorageGridLayout.FillDirection.BY_ROW)
            .build();

    assertFalse(sgl.isValidLocation(1,2));

  }
}
