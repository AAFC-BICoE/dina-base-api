package ca.gc.aafc.dina.workbook;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class WorkbookConverterIT {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  @Test
  public void workbookConverterSheet_onValidFile_ExpectedContentConverted() throws IOException {

    try (InputStream is = WorkbookConverterIT.class.getClassLoader()
        .getResourceAsStream("specimenImporterTemplateTest.xlsx")) {
      List<WorkbookConverter.WorkbookRow> sheet = WorkbookConverter.convertSheet(is, 0);

      assertEquals(3, sheet.size());
      assertEquals("Collection Code *", sheet.get(0).getContent()[0]);

      String jsonStr = OBJECT_MAPPER.writeValueAsString(sheet);

      //null elements should be included as empty string
      assertFalse(jsonStr.contains("null"));
    }
  }

  @Test
  public void workbookConverterWorkbook_onValidFile_ExpectedContentConverted() throws IOException {

    try (InputStream is = WorkbookConverterIT.class.getClassLoader()
        .getResourceAsStream("specimenImporterTemplateTest.xlsx")) {
      Map<Integer, List<WorkbookConverter.WorkbookRow>> workbook = WorkbookConverter.convertWorkbook(is);

      assertEquals(2, workbook.size());
      assertEquals("Collection Code *", workbook.get(0).get(0).getContent()[0]);
      assertEquals("sheet", workbook.get(1).get(0).getContent()[0]);
    }
  }

  @Test
  public void workbookConverter_onValidFileWithEmptyLine_ExpectedContentConverted() throws IOException {
    try (InputStream is = WorkbookConverterIT.class.getClassLoader()
        .getResourceAsStream("empty_first_row.xlsx")) {
      List<WorkbookConverter.WorkbookRow> content = WorkbookConverter.convertSheet(is, 0);
      assertEquals(3, content.size());
      assertEquals("test1", content.get(0).getContent()[0]);
      assertEquals(1, content.get(0).getRowNumber());
    }
  }

  @Test
  public void workbookConverter_onValidFileWithFormatting_ExpectedContentConverted() throws IOException {
    try (InputStream is = WorkbookConverterIT.class.getClassLoader()
        .getResourceAsStream("styled_spreadsheet.xlsx")) {
      List<WorkbookConverter.WorkbookRow> content = WorkbookConverter.convertSheet(is, 0);
      assertEquals(49, content.size());
    }
  }
}
