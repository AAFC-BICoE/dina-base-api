package ca.gc.aafc.dina.workbook;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class WorkbookConverterIT {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  @Test
  public void workbookConverterSheet_onValidFile_ExpectedContentConverted() throws IOException {

    try (InputStream is = WorkbookConverterIT.class.getClassLoader()
        .getResourceAsStream("specimenImporterTemplateTest.xlsx")) {
      WorkbookSheet sheet = WorkbookConverter.convertSheet(is, 0);

      assertEquals(3, sheet.rows().size());
      assertEquals("Collection Code *", sheet.rows().getFirst().content()[0]);

      String jsonStr = OBJECT_MAPPER.writeValueAsString(sheet);

      //null elements should be included as empty string
      assertFalse(jsonStr.contains("null"));
    }
  }

  @Test
  public void workbookConverterWorkbook_onValidFile_ExpectedContentConverted() throws IOException {
    try (InputStream is = WorkbookConverterIT.class.getClassLoader()
        .getResourceAsStream("specimenImporterTemplateTest.xlsx")) {
      Map<Integer, WorkbookSheet> workbook = WorkbookConverter.convertWorkbook(is);
      assertEquals(2, workbook.size());
      List<WorkbookRow> workbookSheet = workbook.get(0).rows();
      assertEquals("Collection Code *", workbookSheet.getFirst().content()[0]);
      assertEquals("sheet", workbookSheet.getFirst().content()[0]);
    }
  }

  @Test
  public void workbookConverter_onValidFileWithEmptyLine_ExpectedContentConverted() throws IOException {
    try (InputStream is = WorkbookConverterIT.class.getClassLoader()
      .getResourceAsStream("empty_second_row.xlsx")) {
      Map<Integer, WorkbookSheet> workbook = WorkbookConverter.convertWorkbook(is);

      assertEquals(1, workbook.size());
      assertEquals(1, workbook.get(0).rows().size());
    }
  }

  @Test
  public void workbookConverter_onValidFileWithEmptyFirstLine_ExpectedContentConverted() throws IOException {
    try (InputStream is = WorkbookConverterIT.class.getClassLoader()
        .getResourceAsStream("empty_first_row.xlsx")) {
      WorkbookSheet workbookSheet = WorkbookConverter.convertSheet(is, 0);
      List<WorkbookRow> content = workbookSheet.rows();
      assertEquals(3, content.size());
      assertEquals("test1", content.getFirst().content()[0]);
      assertEquals(1, content.getFirst().rowNumber());
    }
  }

  @Test
  public void workbookConverter_onValidFileWithFormatting_ExpectedContentConverted() throws IOException {
    try (InputStream is = WorkbookConverterIT.class.getClassLoader()
        .getResourceAsStream("styled_spreadsheet.xlsx")) {
      WorkbookSheet workbookSheet = WorkbookConverter.convertSheet(is, 0);
      assertEquals("Checklist", workbookSheet.sheetName());
      List<WorkbookRow> content = workbookSheet.rows();
      assertEquals(1, content.size());
      assertEquals("green cell", content.getFirst().content()[9]);
    }
  }

  @Test
  public void workbookConverter_onValidFileWithCustomProperties_ExpectedContentConverted() throws IOException {
    try (InputStream is = WorkbookConverterIT.class.getClassLoader()
      .getResourceAsStream("generatedFileWithAliases.xlsx")) {
      WorkbookSheet workbookSheet = WorkbookConverter.convertSheet(is, 0);
      assertNotNull(workbookSheet.originalColumns());
      assertNotNull(workbookSheet.columnAliases());
    }
  }
}
