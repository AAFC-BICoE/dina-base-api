package ca.gc.aafc.dina.workbook;

import java.util.List;
import lombok.Builder;

/**
 * Represents a Workbook Sheet
 * @param rows
 */
@Builder
public record WorkbookSheet(String sheetName, List<WorkbookRow> rows) {

}
