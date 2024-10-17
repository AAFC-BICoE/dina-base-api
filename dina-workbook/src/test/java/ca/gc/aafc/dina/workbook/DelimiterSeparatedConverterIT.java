package ca.gc.aafc.dina.workbook;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Expects exampleTsv.tsv and exampleCsv.csv to have the same content except the separator used.
 */
public class DelimiterSeparatedConverterIT {

  @Test
  public void delimiterSeparatedConverter_onValidTSV_ExpectedContentConverted() throws
    IOException {
    try (InputStream is = WorkbookConverterIT.class.getClassLoader()
      .getResourceAsStream("exampleTsv.tsv")) {
      assertDelimiterSeparated(DelimiterSeparatedConverter.convert(is,
        DelimiterSeparatedConverter.TSV_MEDIA_TYPE));
    }
  }

  @Test
  public void delimiterSeparatedConverter_onValidCSV_ExpectedContentConverted() throws
    IOException {
    try (InputStream is = WorkbookConverterIT.class.getClassLoader()
      .getResourceAsStream("exampleCsv.csv")) {
      assertDelimiterSeparated(DelimiterSeparatedConverter.convert(is,
        DelimiterSeparatedConverter.CSV_MEDIA_TYPE));
    }
  }

  private void assertDelimiterSeparated(WorkbookSheet workbookSheet) {
    List<WorkbookRow> content = workbookSheet.rows();
    // check number of lines
    assertEquals(2, content.size());
    // check number of columns
    assertEquals(6, content.getFirst().content().length);
    // check value with comma inside
    assertEquals("4,5", content.getFirst().content()[4]);
  }
}
