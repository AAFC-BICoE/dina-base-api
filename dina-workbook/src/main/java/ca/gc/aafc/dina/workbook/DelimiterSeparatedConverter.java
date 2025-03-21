package ca.gc.aafc.dina.workbook;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvParser;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;

/**
 * For delimiter-separated files (e.g. CSV/TSV).
 */
public final class DelimiterSeparatedConverter {

  public static final String CSV_MEDIA_TYPE = "text/csv";
  public static final String TSV_MEDIA_TYPE = "text/tab-separated-values";
  public static final Set<String> SUPPORTED_TYPES = Set.of(CSV_MEDIA_TYPE, TSV_MEDIA_TYPE);

  private DelimiterSeparatedConverter() {
    // utility class
  }

  public static boolean isSupported(String mediaType) {
    return SUPPORTED_TYPES.contains(mediaType);
  }

  public static WorkbookSheet convert(InputStream in, String mediaType) throws IOException {
    WorkbookSheet.WorkbookSheetBuilder workbookSheetBuilder = WorkbookSheet.builder();
    CsvMapper mapper = new CsvMapper();
    CsvSchema.Builder schemaBuilder = mapper.schema().rebuild();

    if (TSV_MEDIA_TYPE.equals(mediaType)) {
      schemaBuilder.setColumnSeparator('\t');
    }

    List<WorkbookRow> sheetContent = new ArrayList<>();
    try (MappingIterator<String[]> it = mapper
      .readerForArrayOf(String.class)
      .with(CsvParser.Feature.WRAP_AS_ARRAY)
      .with(schemaBuilder.build())
      .readValues(in)) {

      int rowNumber = 0;
      while (it.hasNextValue()) {
        sheetContent.add(new WorkbookRow(rowNumber, it.nextValue()));
      }
    }
    workbookSheetBuilder.rows(sheetContent);
    return workbookSheetBuilder.build();
  }
}
