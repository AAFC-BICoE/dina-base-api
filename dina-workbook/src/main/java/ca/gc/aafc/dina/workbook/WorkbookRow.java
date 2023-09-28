package ca.gc.aafc.dina.workbook;

import java.util.Arrays;
import lombok.Builder;

/**
 * Represents a single row of a Workbook.
 * @param rowNumber starts at 0
 * @param content
 */
@Builder
public record WorkbookRow(int rowNumber, String[] content) {

  @Override
  public String toString() {
    return rowNumber + ":" + Arrays.toString(content);
  }

}
