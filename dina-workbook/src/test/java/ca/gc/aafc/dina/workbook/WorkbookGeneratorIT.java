package ca.gc.aafc.dina.workbook;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.apache.poi.ss.usermodel.Workbook;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class WorkbookGeneratorIT {

  @Test
  public void generate_withColumnName_workbookGenerated() throws IOException {
    Path tmpExport = Files.createTempDirectory("generate_withColumnName_workbookGenerated")
      .resolve("generatedFile.xlsx");
    try (Workbook wb = WorkbookGenerator.generate(List.of("col 1", "col 2"))) {
      wb.write(new FileOutputStream(tmpExport.toFile()));
    }

    try(FileInputStream fis = new FileInputStream(tmpExport.toFile())) {
      var result = WorkbookConverter.convertWorkbook(fis);
      // check value of the first cell of the first row of the first sheet
      assertEquals("col 1", result.get(0).rows().getFirst().content()[0]);
    }
  }

  @Test
  public void generate_withColumnNameAliases_workbookGenerated() throws IOException {
    Path tmpExport = Files.createTempDirectory("generate_withColumnName_workbookGenerated")
      .resolve("generatedFileWithAliases.xlsx");
    try (Workbook wb = WorkbookGenerator.generate(List.of("col 1", "col 2"),
      List.of("alias 1", "alias 2"))) {
      wb.write(new FileOutputStream(tmpExport.toFile()));
    }

    try (FileInputStream fis = new FileInputStream(tmpExport.toFile())) {
      var result = WorkbookConverter.convertWorkbook(fis);
      // check value of the first cell of the first row of the first sheet
      assertEquals("alias 1", result.get(0).rows().getFirst().content()[0]);
    }
  }
}
