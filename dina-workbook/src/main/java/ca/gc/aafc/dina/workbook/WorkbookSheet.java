package ca.gc.aafc.dina.workbook;

import java.util.List;
import lombok.Builder;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Represents a Workbook Sheet.
 *
 * @param sheetName
 * @param originalColumns
 * @param columnAliases
 * @param rows
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Builder
public record WorkbookSheet(String sheetName, List<String> originalColumns, List<String> columnAliases, List<WorkbookRow> rows) {

}
