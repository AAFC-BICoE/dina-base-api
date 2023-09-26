package ca.gc.aafc.dina.workbook;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DelimiterSeparatedConverterIT {

  @Test
  public void delimiterSeparatedConverter_onValidTSV_ExpectedContentConverted() throws
    IOException {
    try (InputStream is = WorkbookConverterIT.class.getClassLoader()
      .getResourceAsStream("exampleTsv.tsv")) {
      List<WorkbookRow> content = DelimiterSeparatedConverter.convert(is, DelimiterSeparatedConverter.TSV_MEDIA_TYPE);
      // check number of lines
      assertEquals(2, content.size());
      // check number of columns
      assertEquals(6, content.get(0).content().length);
      // check value with comma inside
      assertEquals("4,5", content.get(0).content()[4]);
    }
  }
}
