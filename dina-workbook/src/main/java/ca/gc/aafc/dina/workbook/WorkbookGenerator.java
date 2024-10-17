package ca.gc.aafc.dina.workbook;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
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

  public static final String WORKBOOK_CUSTOM_PROPS_COLUMNS = "originalColumns";
  public static final String WORKBOOK_CUSTOM_PROPS_ALIASES = "columnAliases";

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
    return generate(columns, null);
  }

  /**
   * Generate a workbook from a list of column names and aliases
   * Use in a try-with-resource.
   *
   * @param columns columns name
   * @param columnAliases aliases to use as column names
   * @return the Workbook object
   */
  public static Workbook generate(List<String> columns, List<String> columnAliases) {
    XSSFWorkbook wb = new XSSFWorkbook();
    Sheet sheet1 = wb.createSheet();

    boolean useColumnAliases = CollectionUtils.isNotEmpty(columnAliases);

    // Record in custom properties the original columns
    POIXMLProperties.CustomProperties customProp = wb.getProperties().getCustomProperties();
    customProp.addProperty(WORKBOOK_CUSTOM_PROPS_COLUMNS, String.join(",", columns));

    if (useColumnAliases) {
      customProp.addProperty(WORKBOOK_CUSTOM_PROPS_ALIASES,
        columnAliases.stream().map(s -> s.replace(",", "_"))
          .collect(Collectors.joining(",")));
    }

    // Rows are 0 based
    Row row = sheet1.createRow(0);
    List<String> columnNames = useColumnAliases ? columnAliases : columns;
    int cellIdx = 0;
    for (String columnName: columnNames) {
      row.createCell(cellIdx).setCellValue(columnName);
      cellIdx++;
    }
    return wb;
  }
}
