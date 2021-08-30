package ca.gc.aafc.dina.workbook;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class WorkbookConverterIT {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  @Test
  public void workbookConverter_onValidFile_ExpectedContentConverted() throws IOException {

    try (InputStream is = WorkbookConverterIT.class.getClassLoader()
        .getResourceAsStream("specimenImporterTemplateTest.xlsx")) {
      List<WorkbookConverter.WorkbookRow> content = WorkbookConverter.convert(is);

      assertEquals(3, content.size());
      assertEquals("Collection Code *", content.get(0).getContent()[0]);

      String jsonStr = OBJECT_MAPPER.writeValueAsString(content);

      //null elements should be included as empty string
      assertFalse(jsonStr.contains("null"));

    }
  }

  @Test
  public void workbookConverter_onValidFileWithEmptyLine_ExpectedContentConverted() throws IOException {
    try (InputStream is = WorkbookConverterIT.class.getClassLoader()
        .getResourceAsStream("empty_first_row.xlsx")) {
      List<WorkbookConverter.WorkbookRow> content = WorkbookConverter.convert(is);
      assertEquals(3, content.size());
      assertEquals("test1", content.get(0).getContent()[0]);
      assertEquals(1, content.get(0).getRowNumber());
    }
  }
}
