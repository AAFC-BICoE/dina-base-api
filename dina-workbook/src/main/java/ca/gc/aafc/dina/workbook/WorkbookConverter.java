package ca.gc.aafc.dina.workbook;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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
   * Converts a Workbook and return a Map where the key is the sheet number (starting at 0) and
   * the value a list of WorkbookRow.
   * The method will use the string value of each cells.
   * The entire workbook will be loaded in memory.
   * @param in will not be closed by this method
   * @return map of sheet and list of all rows or an empty map (never null)
   * @throws IOException
   */
  public static Map<Integer, WorkbookSheet> convertWorkbook(InputStream in) throws IOException {
    Map<Integer, WorkbookSheet> workbookContent = new HashMap<>();
    Workbook book = WorkbookFactory.create(in);
    for (int i = 0; i < book.getNumberOfSheets(); i++) {
      workbookContent.put(i, convertSheet(book.getSheetAt(i)));
    }
    return workbookContent;
  }

  /**
   * Converts a specific sheet of a Workbook into a list of WorkbookRow.
   * @param in will not be closed by this method
   * @param sheetNumber sheet to concert, starts at 0
   * @return list of all rows or an empty list (never null)
   */
  public static WorkbookSheet convertSheet(InputStream in, int sheetNumber) throws IOException {
    Workbook book = WorkbookFactory.create(in);
    return convertSheet(book.getSheetAt(sheetNumber));
  }

  /**
   * Converts a specific {@link Sheet} into a list of {@link WorkbookRow}.
   * The method will use the string value of each cells.
   * The entire sheet will be loaded in memory.
   * Rows that are completely empty will be skipped.
   * @param sheet the {@link Sheet} to convert
   * @return {@link WorkbookSheet} that contains a list of {@link WorkbookRow}
   * with sheet content or empty list (never null).
   */
  private static WorkbookSheet convertSheet(Sheet sheet) {
    WorkbookSheet.WorkbookSheetBuilder workbookSheetBuilder = WorkbookSheet.builder();

    List<WorkbookRow> sheetContent = new ArrayList<>();
    workbookSheetBuilder.sheetName(sheet.getSheetName());
    for (Row row : sheet) {
      String[] content = new String[row.getLastCellNum() > 0 ? row.getLastCellNum() : 0];
      boolean rowHasData = false;
      for (int i = 0; i < row.getLastCellNum(); i++) {
        // do not include null in the content array, use empty string instead.
        content[i] = Objects.toString(SpreadsheetHelper.getCellAsString(row.getCell(i)), "");
        // check is we found at least 1 piece of data on the row
        rowHasData = rowHasData || !"".equals(content[i]);
      }

      if (rowHasData) {
        WorkbookRow currWorkbookRow = WorkbookRow.builder()
          .rowNumber(row.getRowNum())
          .content(content)
          .build();
        sheetContent.add(currWorkbookRow);
      }
    }
    return workbookSheetBuilder.rows(sheetContent).build();
  }

}
