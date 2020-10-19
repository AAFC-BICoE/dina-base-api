package ca.gc.aafc.dina.workbook;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class WorkbookConverter {

  // Utility class
  private WorkbookConverter() {}

  /**
   * Converts the first sheet of a Workbook into a list of WorkbookRow.
   * The method will use the string value of each cells.
   * The entire sheet will be loaded in memory.
   * Only the sheet 0 of the Workbook will be converted.
   * @param in will not be closed by this method
   * @return list of all rows or an empty list (never null)
   * @throws IOException
   */
  public static List<WorkbookRow> convert(InputStream in) throws IOException {
    List<WorkbookRow> sheetContent = new ArrayList<>();
    Workbook book = WorkbookFactory.create(in);
    Sheet sheet = book.getSheetAt(0); // only convert sheet 0

    // The number of column is evaluated from the first row
    Row firstRow = sheet.getRow(0);
    short expectedNumberOfColumn = firstRow.getLastCellNum();

    for (Row row : sheet) {
      String[] content = new String[expectedNumberOfColumn];
      for (int i = 0; i < expectedNumberOfColumn; i++) {
        // do not include null in the content array, use empty string instead.
        content[i] = Objects.toString(SpreadsheetHelper.getCellAsString(row.getCell(i)), "");
      }
      WorkbookRow currWorkbookRow = WorkbookRow.builder()
          .rowNumber(row.getRowNum())
          .content(content)
          .build();
      sheetContent.add(currWorkbookRow);
    }

    return sheetContent;
  }

  @Builder
  @Getter
  public static final class WorkbookRow {
    private final int rowNumber;

    private final String[] content;

    @Override
    public String toString() {
      return rowNumber + ":" + Arrays.toString(content);
    }
  }
}
