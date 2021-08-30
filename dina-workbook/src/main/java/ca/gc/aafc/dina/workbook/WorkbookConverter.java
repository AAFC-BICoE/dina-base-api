package ca.gc.aafc.dina.workbook;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Builder;
import lombok.Getter;
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

@SuppressFBWarnings({ "EI_EXPOSE_REP", "EI_EXPOSE_REP2" })
public final class WorkbookConverter {

  private static final String SUPPORTED_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

  // Utility class
  private WorkbookConverter() { }

  /**
   * Check if a mediaType is supported as source for the WorkbookConverter.
   * @param mediaType
   * @return
   */
  public static boolean isSupported(String mediaType) {
    return SUPPORTED_TYPE.equals(mediaType);
  }

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

    for (Row row : sheet) {
      String[] content = new String[row.getLastCellNum() > 0 ? row.getLastCellNum() : 0];
      for (int i = 0; i < row.getLastCellNum(); i++) {
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
