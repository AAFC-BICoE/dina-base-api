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
    assertTrue(sgl.isValidRow(5));
    assertTrue(sgl.isValidColumn(5));

    // numbers are 1-based
    assertFalse(sgl.isValidLocation(0, 0));
    assertFalse(sgl.isValidRow(0));
    assertFalse(sgl.isValidColumn(0));

    assertFalse(sgl.isValidLocation(5, 6));

    // StorageGridLayout invalid state
    sgl = StorageGridLayout.builder()
            .numberOfRows(null)
            .numberOfColumns(null)
            .fillDirection(StorageGridLayout.FillDirection.BY_ROW)
            .build();

    assertFalse(sgl.isValidLocation(1,2));
  }

  @Test
  public void testStorageGridLayoutCalculateCellNumber() {
    StorageGridLayout sgl = StorageGridLayout.builder()
            .numberOfRows(5)
            .numberOfColumns(5)
            .fillDirection(StorageGridLayout.FillDirection.BY_ROW)
            .build();

    assertEquals(1, sgl.calculateCellNumber(1,1));
    assertEquals(8, sgl.calculateCellNumber(2,3));
    assertEquals(25, sgl.calculateCellNumber(5,5));

    //test by column
    sgl.setFillDirection(StorageGridLayout.FillDirection.BY_COLUMN);
    assertEquals(1, sgl.calculateCellNumber(1,1));
    assertEquals(12, sgl.calculateCellNumber(2,3));
    assertEquals(25, sgl.calculateCellNumber(5,5));

    // test exception
    assertThrows(IllegalArgumentException.class, ()-> sgl.calculateCellNumber(5,25));
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
