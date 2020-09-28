package ca.gc.aafc.dina.workbook;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class WorkbookConverterTest {

  @Test
  public void workbookConverter_onValidFile_ExpectedContentConverted() throws IOException {

    try (InputStream is = WorkbookConverterTest.class.getClassLoader()
        .getResourceAsStream("specimenImporterTemplateTest.xlsx")) {
      List<WorkbookConverter.WorkbookRow> content = WorkbookConverter.convert(is);

      assertEquals(3, content.size());
      assertEquals("Collection Code *", content.get(0).getContent()[0]);

    }



  }
}
