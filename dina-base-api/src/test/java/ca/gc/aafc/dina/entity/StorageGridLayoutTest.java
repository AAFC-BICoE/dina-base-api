package ca.gc.aafc.dina.entity;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

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


  @ParameterizedTest
  @ValueSource(strings = { "r0w", "2125", "col umn", "#%#&!", "" })
  public void fillDirectionFromString_OnInvalidInput_ReturnsEmptyOptional(String input) {
    assertEquals(Optional.empty(), StorageGridLayout.FillDirection.fromString(input));
  }

  @ParameterizedTest
  @ValueSource(strings = { "row", "BY ROW" })
  public void fillDirectionFromString_OnValidRowInput_ReturnFillDirectionByRow(String input) {
    assertEquals(StorageGridLayout.FillDirection.BY_ROW, StorageGridLayout.FillDirection.fromString(input).orElseThrow());
  }

  @ParameterizedTest
  @ValueSource(strings = { "col", "bY cOlUmN" })
  public void fillDirectionFromString_OnValidColumnInput_ReturnFillDirectionByColumn(String input) {
    assertEquals(StorageGridLayout.FillDirection.BY_COLUMN, StorageGridLayout.FillDirection.fromString(input).orElseThrow());
  }
}
