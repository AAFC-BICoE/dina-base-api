package ca.gc.aafc.dina.entity;

import lombok.Builder;
import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Represents a very general storage grid layout with row (x-axis), column (y-axis) and FillDirection.
 */
@Data
@Builder
public class StorageGridLayout {

  @NotNull
  @Min(1)
  @Max(1000)
  private Integer numberOfRows;

  @NotNull
  @Min(1)
  @Max(1000)
  private Integer numberOfColumns;

  @NotNull
  private FillDirection fillDirection;

  public enum FillDirection {
    BY_ROW("by row"), BY_COLUMN("by column");

    private static final Pattern COLUMN_REGEX = Pattern.compile("(?:by[_ ])?col(?:umn)?");
    private static final Pattern ROW_REGEX = Pattern.compile("(?:by[_ ])?row");

    private final String text;

    FillDirection(String text) {
      this.text = text;
    }

    public String getText() {
      return this.text;
    }

    /**
     * Null safe method that returns the FillDirection from a string input. Optional container forces
     * caller to handle null values. Input string is not case-sensitive.
     *
     * @param text
     *          input string, for example, "By row"
     * @return Optional FillDirection object
     */
    public static Optional<FillDirection> fromString(String text) {
      if(ROW_REGEX.matcher(text.toLowerCase()).matches()) {
        return Optional.of(BY_ROW);
      } else if (COLUMN_REGEX.matcher(text.toLowerCase()).matches()) {
        return Optional.of(BY_COLUMN);
      }
      return Optional.empty();
    }
  }

  /**
   * Checks if the provided column and row numbers are valid for the current grid layout.
   * A valid location doesn't mean a non-occupied location.
   * Note: if the grid layout is in invalid state (numberOfRows or numberOfColumns is null), this method will return false.
   * @param rowNumber 1-based row number
   * @param columnNumber 1-based column number
   * @return true if the row number and column number are valid for the grid layout. false otherwise.
   */
  public boolean isValidLocation(int rowNumber, int columnNumber) {
    return isValidRow(rowNumber) && isValidColumn(columnNumber);
  }

  /**
   * Checks if the provided row number is valid for the current grid layout.
   * Note: if the grid layout is in invalid state (numberOfRows is null), this method will return false.
   * @param rowNumber 1-based row number
   * @return true if the row number is valid for the grid layout. false otherwise.
   */
  public boolean isValidRow(int rowNumber) {
    // numbers start at 1
    if (rowNumber <= 0 || numberOfRows == null) {
      return false;
    }
    return rowNumber <= numberOfRows;
  }

  /**
   * Checks if the provided column number is valid for the current grid layout.
   * Note: if the grid layout is in invalid state (numberOfColumns is null), this method will return false.
   * @param columnNumber 1-based column number
   * @return true if the column number is valid for the grid layout. false otherwise.
   */
  public boolean isValidColumn(int columnNumber) {
    // numbers start at 1
    if (columnNumber <= 0 || numberOfColumns == null) {
      return false;
    }
    return columnNumber <= numberOfColumns;
  }

  /**
   * Calculates the cell number given row and column location.
   * The {@link #fillDirection} will determine how the cell number is calculated.
   *
   * Given a 3x3 square, reading it left to right, by row, cell numbers will be:
   *
   * BY_ROW: 1 2 3 4 5 6 7 8 9
   * BY_COLUMN: 1 4 7 2 5 8 3 6 9
   *
   * @param rowNumber
   * @param columnNumber
   * @return the cell number or {@link IllegalArgumentException} for invalid arguments
   */
  public int calculateCellNumber(int rowNumber, int columnNumber) {
    Objects.requireNonNull(fillDirection);
    Objects.requireNonNull(numberOfRows);
    Objects.requireNonNull(numberOfColumns);

    if (!isValidLocation(rowNumber, columnNumber)) {
      throw new IllegalArgumentException("Invalid row/column");
    }

    return switch (fillDirection) {
      case BY_ROW -> (((rowNumber - 1) * numberOfColumns) + columnNumber);
      case BY_COLUMN -> (((columnNumber - 1) * numberOfRows) + rowNumber);
    };
  }

}
