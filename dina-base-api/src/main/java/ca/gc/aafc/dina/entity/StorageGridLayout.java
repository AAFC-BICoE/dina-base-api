package ca.gc.aafc.dina.entity;

import lombok.Builder;
import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.util.Optional;
import java.util.regex.Pattern;

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
   * @return true if the number of rows and columns is valid for the grid layout. false otherwise.
   */
  public boolean isValidLocation(int rowNumber, int columnNumber) {
    // numbers start at 1
    if (rowNumber <= 0 || columnNumber <= 0 || numberOfRows == null || numberOfColumns == null) {
      return false;
    }

    return rowNumber <= numberOfRows && columnNumber <= numberOfColumns;
  }
}
