package ca.gc.aafc.dina.workbook;

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ooxml.POIXMLProperties;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.openxmlformats.schemas.officeDocument.x2006.customProperties.CTProperty;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static ca.gc.aafc.dina.workbook.WorkbookGenerator.WORKBOOK_CUSTOM_PROPS_ALIASES;
import static ca.gc.aafc.dina.workbook.WorkbookGenerator.WORKBOOK_CUSTOM_PROPS_COLUMNS;

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

    try (XSSFWorkbook book = new XSSFWorkbook(in, false)) {
      for (int i = 0; i < book.getNumberOfSheets(); i++) {
        workbookContent.put(i, convertSheet(book, book.getSheetAt(i)));
      }
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
    WorkbookSheet workbookSheet;
    try (XSSFWorkbook book = new XSSFWorkbook(in, false)) {
      workbookSheet = convertSheet(book, book.getSheetAt(sheetNumber));
    }
    return workbookSheet;
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
  private static WorkbookSheet convertSheet(XSSFWorkbook book, Sheet sheet) {
    WorkbookSheet.WorkbookSheetBuilder workbookSheetBuilder = WorkbookSheet.builder();

    extractDinaMetadata(book, workbookSheetBuilder);

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

  /**
   * Extract dina specific metadata from the Workbook that may be present if the Workbook was
   * created by {@link WorkbookGenerator}.
   * @param book
   * @param workbookSheetBuilder
   */
  private static void extractDinaMetadata(XSSFWorkbook book,
                                          WorkbookSheet.WorkbookSheetBuilder workbookSheetBuilder) {
    POIXMLProperties.CustomProperties customProperties = book.getProperties().getCustomProperties();
    if (customProperties == null) {
      return;
    }

    CTProperty columns = customProperties.getProperty(WORKBOOK_CUSTOM_PROPS_COLUMNS);
    if (columns != null) {
      workbookSheetBuilder.originalColumns(Arrays.asList(StringUtils.split(columns.getLpwstr(), ',')));
    }

    CTProperty aliases = customProperties.getProperty(WORKBOOK_CUSTOM_PROPS_ALIASES);
    if (aliases != null) {
      workbookSheetBuilder.columnAliases(Arrays.asList(StringUtils.split(aliases.getLpwstr(), ',')));
    }
  }
}
