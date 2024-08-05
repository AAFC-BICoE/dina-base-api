package ca.gc.aafc.dina.workbook;

import java.util.List;

import org.apache.poi.ooxml.POIXMLProperties;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * Utility to generates Workbook.
 *
 */
public final class WorkbookGenerator {

  private WorkbookGenerator() {
    //utility class
  }

  /**
   * Generate a workbook from a list of column names.
   * Use in a try-with-resource.
   *
   * @param columns columns name
   * @return the Workbook object
   */
  public static Workbook generate(List<String> columns) {
    XSSFWorkbook wb = new XSSFWorkbook();
    Sheet sheet1 = wb.createSheet();

    // Record in custom properties the original columns
    POIXMLProperties.CustomProperties customProp = wb.getProperties().getCustomProperties();
    customProp.addProperty("originalColumns", String.join(",", columns));

    // Rows are 0 based
    Row row = sheet1.createRow(0);

    int cellIdx = 0;
    for (String columnName: columns) {
      row.createCell(cellIdx).setCellValue(columnName);
      cellIdx++;
    }

    return wb;
  }
}
