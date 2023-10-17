package ca.gc.aafc.dina.workbook;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.sql.Date;
import java.time.LocalDate;
import java.util.Optional;
import java.util.function.Function;

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Unit test related to {@link SpreadsheetHelper}.
 */
public class SpreadsheetHelperTest {

  private static Row createTestWorkbookRow() {
    try (Workbook wb = new XSSFWorkbook()) {
      Sheet sheet = wb.createSheet("new sheet");
      return sheet.createRow(0);
    } catch (IOException e) {
      fail(e.getMessage());
    }
    return null;
  }

  private static Cell createTestWorkbookCell() {
    return createTestWorkbookRow().createCell(0);
  }

  /**
   * Simply ensure that the provided function throw an exception and that the message of the
   * exception is not blank. Useful to test custom exception instances.
   *
   * @param getCellAsFunction
   * @param cell
   */
  private static void assertIllegalArgumentExceptionMessageIsNotBlank(
      Function<Cell, Object> getCellAsFunction, Cell cell) {
    try {
      getCellAsFunction.apply(cell);
      fail("The provided function is expected to throw an IllegalArgumentException");
    } catch (IllegalArgumentException e) {
      assertFalse(StringUtils.isBlank(e.getMessage()), "The message of the exception is not blank");
    }
  }

  @Test
  public void checkIfRowIsEmpty_EmptyRow_ReturnsTrue() {
    Row row = createTestWorkbookRow();
    assertTrue(SpreadsheetHelper.checkIfRowIsEmpty(row));
  }

  @Test
  public void checkIfRowIsEmpty_NonEmptyRow_ReturnsFalse() {
    Row row = createTestWorkbookRow();
    Cell cell = row.createCell(0);
    cell.setCellValue(true);
    assertFalse(SpreadsheetHelper.checkIfRowIsEmpty(row));
  }

  @Test
  public void checkIfRowIsEmpty_NullRow_ReturnsTrue() {
    assertTrue(SpreadsheetHelper.checkIfRowIsEmpty(null));
  }

  // getCellAsBoolean Tests

  @Test
  public void getCellAsBoolean_BooleanValue_ReturnMatchingBoolean() {
    Cell cell = createTestWorkbookCell();
    cell.setCellValue(true);
    assertTrue(SpreadsheetHelper.getCellAsBoolean(cell).get());

    cell.setCellValue(false);
    assertFalse(SpreadsheetHelper.getCellAsBoolean(cell).get());
  }

  @ParameterizedTest
  @ValueSource(strings = { "true", "yes", "1" })
  public void getCellAsBoolean_TrueAsString_ReturnTrue(String input) {
    Cell cell = createTestWorkbookCell();
    cell.setCellValue(input);
    assertTrue(SpreadsheetHelper.getCellAsBoolean(cell).get());
  }

  @ParameterizedTest
  @ValueSource(strings = { "false", "no", "0" })
  public void getCellAsBoolean_FalseAsString_ReturnFalse(String input) {
    Cell cell = createTestWorkbookCell();
    cell.setCellValue(input);
    assertFalse(SpreadsheetHelper.getCellAsBoolean(cell).get());
  }

  @Test
  public void getCellAsBoolean_EmptyCell_ReturnEmpty() {
    Cell cell = createTestWorkbookCell();
    assertEquals(Optional.empty(), SpreadsheetHelper.getCellAsBoolean(cell));

    cell = createTestWorkbookCell();
    cell.setCellValue("");
    assertEquals(Optional.empty(), SpreadsheetHelper.getCellAsBoolean(cell));
  }

  @Test
  public void getCellAsBoolean_StringCell_ReturnIllegalArgumentException() {
    Cell cell = createTestWorkbookCell();
    cell.setCellValue("bluE");
    assertThrows(IllegalArgumentException.class, () -> SpreadsheetHelper.getCellAsBoolean(cell));
  }

  @Test
  public void getCellAsBoolean_StringCell_IllegalArgumentExceptionContainsMessage() {
    Cell cell = createTestWorkbookCell();
    cell.setCellValue("bluE");
    assertIllegalArgumentExceptionMessageIsNotBlank(SpreadsheetHelper::getCellAsBoolean, cell);
  }

  @Test
  public void getCellAsBoolean_NullCell_ReturnEmptyOptional() {
    assertEquals(Optional.empty(), SpreadsheetHelper.getCellAsBoolean(null));
  }

  // getCellAsDate Tests

  @Test
  public void getCellAsDate_DateCell_ReturnCorrectDate() {
    Cell cell = createTestWorkbookCell();
    java.sql.Date date = java.sql.Date.valueOf(LocalDate.now());
    CellStyle dateStyle = cell.getRow().getSheet().getWorkbook().createCellStyle();
    // Set data format to mimic a date
    dateStyle.setDataFormat((short) 14);
    cell.setCellStyle(dateStyle);
    cell.setCellValue(date);
    assertEquals(date, SpreadsheetHelper.getCellAsDate(cell));

    // Test date provided as ISO string
    LocalDate testDate = LocalDate.of(2018, 10, 13);
    cell = createTestWorkbookCell();
    cell.setCellValue(testDate.toString());
    assertEquals(java.sql.Date.valueOf(testDate), SpreadsheetHelper.getCellAsDate(cell));
  }

  @ParameterizedTest
  @ValueSource(strings = { "01-02-2018", "01022018", "02/15/1985", "electricBoggalu" })
  public void getCellAsDate_NonISODate_ThrowsIllegalArgumentException(String input) {
    Cell cell = createTestWorkbookCell();
    cell.setCellValue(input);
    assertThrows(IllegalArgumentException.class, () -> SpreadsheetHelper.getCellAsDate(cell));
  }

  @Test
  public void getCellAsDate_InValidNumericInput_ThrowsException() {
    Cell cell = createTestWorkbookCell();
    cell.setCellValue(5432);
    assertThrows(IllegalArgumentException.class, () -> SpreadsheetHelper.getCellAsDate(cell));
  }

  @ParameterizedTest
  @ValueSource(strings = { " ", "   ", "\t", "\n" })
  @NullAndEmptySource
  public void getCellAsDate_BlankStringCell_ReturnNull(String input) {
    Cell cell = createTestWorkbookCell();
    cell.setCellValue(input);
    assertNull(SpreadsheetHelper.getCellAsDate(cell));
  }

  @Test
  public void getCellAsInteger_ValidCell_ReturnInteger() {
    Cell cell = createTestWorkbookCell();
    cell.setCellValue(416);
    assertEquals(416, SpreadsheetHelper.getCellAsInteger(cell).get().intValue());

    cell = createTestWorkbookCell();
    cell.setCellValue("416");
    assertEquals(416, SpreadsheetHelper.getCellAsInteger(cell).get().intValue(),
        "Integer value provided as String returns Integer value");
  }

  @Test
  public void getCellAsInteger_DoubleValue_ThrowsNumberFormatException() {
    Cell cell = createTestWorkbookCell();
    cell.setCellValue(416.01);
    assertThrows(NumberFormatException.class, () -> SpreadsheetHelper.getCellAsInteger(cell));
  }

  @Test
  public void getCellAsInteger_NonIntegerCell_ThrowsNumberFormatException() {
    Cell cell = createTestWorkbookCell();
    cell.setCellValue("asd");
    assertThrows(NumberFormatException.class, () -> SpreadsheetHelper.getCellAsInteger(cell));
  }

  @ParameterizedTest
  @ValueSource(strings = { " ", "   ", "\t", "\n" })
  @NullAndEmptySource
  public void getCellAsInteger_EmptyCell_ReturnEmptyOptional(String input) {
    Cell cell = createTestWorkbookCell();
    cell.setCellValue(input);
    assertEquals(Optional.empty(), SpreadsheetHelper.getCellAsInteger(cell));
  }

  @Test
  public void getCellAsDouble_DoubleCell_ReturnDouble() {
    Cell cell = createTestWorkbookCell();
    cell.setCellValue(416.1);
    assertEquals(416.1, SpreadsheetHelper.getCellAsDouble(cell).get().doubleValue(), 0);
  }

  @ParameterizedTest
  @ValueSource(strings = { "416.1", "416"})
  public void getCellAsDouble_DoubleFromString_ReturnDouble(String input) {
    Cell cell = createTestWorkbookCell();
    cell.setCellValue(input);
    double result = SpreadsheetHelper.getCellAsDouble(cell).get().doubleValue();
    assertEquals(Double.parseDouble(input), result, 0);
  }

  @ParameterizedTest
  @ValueSource(strings = { " ", "   ", "\t", "\n" })
  @NullAndEmptySource
  public void getCellAsDouble_EmptyCell_ReturnEmptyOptional(String input) {
    Cell cell = createTestWorkbookCell();
    cell.setCellValue(input);
    assertEquals(Optional.empty(), SpreadsheetHelper.getCellAsDouble(cell));
  }

  @Test
  public void getCellAsDouble_StringCell_ReturnNumberFormatException() {
    Cell cell = createTestWorkbookCell();
    cell.setCellValue("string");
    assertThrows(NumberFormatException.class, () -> SpreadsheetHelper.getCellAsDouble(cell));
  }

  @Test
  public void getCellAsString_PopulatedCell_ReturnValueAsString() {
    Cell cell = createTestWorkbookCell();
    cell.setCellValue("String");
    assertEquals("String", SpreadsheetHelper.getCellAsString(cell));

    cell = createTestWorkbookCell();
    cell.setCellValue(true);
    assertEquals("true", SpreadsheetHelper.getCellAsString(cell));

    cell = createTestWorkbookCell();
    cell.setCellValue(42);
    assertEquals("42", SpreadsheetHelper.getCellAsString(cell));

    //numeric date
    java.sql.Date date = java.sql.Date.valueOf(LocalDate.of(2020,2, 4));
    CellStyle dateStyle = cell.getRow().getSheet().getWorkbook().createCellStyle();
    // Set data format to mimic a date
    dateStyle.setDataFormat((short) 14);
    cell.setCellStyle(dateStyle);
    cell.setCellValue(date);
    assertEquals("2020-02-04", SpreadsheetHelper.getCellAsString(cell));
  }

  @Test
  public void getCellAsString_NullCell_ReturnsNull() {
    assertNull(SpreadsheetHelper.getCellAsString(null));
  }

  @Test
  public void getCellAsString_EmptyCell_ReturnsNull() {
    Cell cell = createTestWorkbookCell();
    assertNull(SpreadsheetHelper.getCellAsString(cell));
  }

  @ParameterizedTest
  @ValueSource(strings = { " ", "   ", "\t", "\n" })
  public void getCellAsString_BlankStringCell_ReturnsEmptyString(String input) {
    Cell cell = createTestWorkbookCell();
    cell.setCellValue(input);
    assertEquals("", SpreadsheetHelper.getCellAsString(cell));
  }

  // Formula Cell tests

  @Test
  public void getCellAsString_FormulaCell_ReturnsNumericAsString() {
    // Creates cell holding formula
    Cell cellF = createTestWorkbookCell();
    cellF.setCellFormula("SUM(B1+B1)");

    FormulaEvaluator evaluator = cellF.getSheet().getWorkbook().getCreationHelper().createFormulaEvaluator();

    // Creates cell with value to be used in formula
    Cell cell = cellF.getRow().createCell(1);
    cell.setCellValue(4);

    // Evaluates formula
    evaluator.evaluateFormulaCell(cellF);

    assertEquals("8", SpreadsheetHelper.getCellAsString(cellF));
  }

  @Test
  public void getCellAsString_FormulaCell_ReturnsString(){
    Cell cellF = createTestWorkbookCell();
    cellF.setCellFormula("RIGHT(B1,LEN(B1)-3)");

    FormulaEvaluator evaluator = cellF.getSheet().getWorkbook().getCreationHelper().createFormulaEvaluator();

    Cell cell = cellF.getRow().createCell(1);
    cell.setCellValue("QWEJava");

    evaluator.evaluateFormulaCell(cellF);

    assertEquals("Java", SpreadsheetHelper.getCellAsString(cellF));
  }

  @Test
  public void getCellAsString_FormulaCellNOTGreater_ReturnsBooleanAsString() {
    Cell cellF = createTestWorkbookCell();
    cellF.setCellFormula("NOT(B1>5)");

    FormulaEvaluator evaluator = cellF.getSheet().getWorkbook().getCreationHelper().createFormulaEvaluator();

    Cell cell = cellF.getRow().createCell(1);
    cell.setCellValue(4);

    evaluator.evaluateFormulaCell(cellF);

    assertEquals("true", SpreadsheetHelper.getCellAsString(cellF));
  }

  @Test
  public void getCellAsString_InvalidFormulaOnString_ReturnsNull(){
    Cell cellF = createTestWorkbookCell();
    cellF.setCellFormula("RIGHT(B1,LEN(B1)-3)");

    FormulaEvaluator evaluator = cellF.getSheet().getWorkbook().getCreationHelper().createFormulaEvaluator();

    Cell cell = cellF.getRow().createCell(1);
    cell.setCellValue("va");

    evaluator.evaluateFormulaCell(cellF);

    assertEquals(null, SpreadsheetHelper.getCellAsString(cellF));
  }

  @Test
  public void getCellAsInteger_FormulaCell_ReturnsInteger() {
    Cell cellF = createTestWorkbookCell();
    cellF.setCellFormula("SUM(B1+B1)");

    FormulaEvaluator evaluator = cellF.getSheet().getWorkbook().getCreationHelper().createFormulaEvaluator();

    Cell cell = cellF.getRow().createCell(1);
    cell.setCellValue(3);

    evaluator.evaluateFormulaCell(cellF);

    assertEquals(Optional.of(6), SpreadsheetHelper.getCellAsInteger(cellF));
  }

  @Test
  public void getCellAsInteger_FormulaCell_ReturnsDouble() {
    Cell cellF = createTestWorkbookCell();
    cellF.setCellFormula("SUM(B1+B1)");

    FormulaEvaluator evaluator = cellF.getSheet().getWorkbook().getCreationHelper().createFormulaEvaluator();

    Cell cell = cellF.getRow().createCell(1);
    cell.setCellValue(3);

    evaluator.evaluateFormulaCell(cellF);

    assertEquals(Optional.of(6.0), SpreadsheetHelper.getCellAsDouble(cellF));
  }

  @Test
  public void getCellAsInteger_InvalidFormulaCell_ReturnsEmpty() {
    Cell cellF = createTestWorkbookCell();
    cellF.setCellFormula("SUM(B1+B1)");

    FormulaEvaluator evaluator = cellF.getSheet().getWorkbook().getCreationHelper().createFormulaEvaluator();

    Cell cell = cellF.getRow().createCell(1);
    cell.setCellValue("d");

    evaluator.evaluateFormulaCell(cellF);

    assertEquals(Optional.empty(), SpreadsheetHelper.getCellAsInteger(cellF));
  }

  @Test
  public void getCellAsBoolean_FormulaCellNotGreater_ReturnsBoolean() {
    Cell cellF = createTestWorkbookCell();
    cellF.setCellFormula("NOT(B1>5)");

    FormulaEvaluator evaluator = cellF.getSheet().getWorkbook().getCreationHelper().createFormulaEvaluator();

    Cell cell = cellF.getRow().createCell(1);
    cell.setCellValue(4);

    evaluator.evaluateFormulaCell(cellF);

    assertEquals(true, SpreadsheetHelper.getCellAsBoolean(cellF).get().booleanValue());
  }

  @Test
  public void getCellAsBoolean_FormulaCellExact_ReturnsBoolean() {
    Cell cellF = createTestWorkbookCell();
    cellF.setCellFormula("EXACT(B1,C1)");

    FormulaEvaluator evaluator = cellF.getSheet().getWorkbook().getCreationHelper().createFormulaEvaluator();

    Cell cell = cellF.getRow().createCell(1);
    cell.setCellValue(4);

    Cell cell2 = cellF.getRow().createCell(2);
    cell2.setCellValue(2);

    evaluator.evaluateFormulaCell(cellF);

    assertEquals(false, SpreadsheetHelper.getCellAsBoolean(cellF).get().booleanValue());
  }

  /**
   * Test getCellAsLocalDate with a valid date string, should return a valid local date instance.
   */
  @Test
  public void getCellAsLocalDate_ValidDateString_ReturnsLocalDate() {
    Cell cell = createTestWorkbookCell();
    cell.setCellValue("2012-12-21");
    assertEquals(LocalDate.of(2012, 12, 21), SpreadsheetHelper.getCellAsLocalDate(cell));
  }

  /**
   * Test getCellAsLocalDate with a valid {@link Date}, should return a valid local date instance.
   */
  @Test
  public void getCellAsLocalDate_ValidDateCell_ReturnsLocalDate() {
    Cell cell = createTestWorkbookCell();
    CellStyle dateStyle = cell.getRow().getSheet().getWorkbook().createCellStyle();
    // Set data format to mimic a date
    dateStyle.setDataFormat((short) 14);
    cell.setCellStyle(dateStyle);
    cell.setCellValue(Date.valueOf(LocalDate.of(2012, 12, 21)));
    assertEquals(LocalDate.of(2012, 12, 21), SpreadsheetHelper.getCellAsLocalDate(cell));
  }

  @ParameterizedTest
  @ValueSource(strings = { " ", "   ", "\t", "\n" })
  @NullAndEmptySource
  public void getCellAsLocalDate_BlankStringCell_ReturnNull(String input) {
    Cell cell = createTestWorkbookCell();
    cell.setCellValue(input);
    assertNull(SpreadsheetHelper.getCellAsLocalDate(cell));
  }

  @ParameterizedTest
  @ValueSource(strings = {
      "01-02-2018", "01022018", "02/15/1985",
      "2012/12/21", "2012 12 21", "2012,12,21",
      "electricBoggalu" })
  public void getCellAsLocalDate_NonISODate_ThrowsIllegalArgumentException(String input) {
    Cell cell = createTestWorkbookCell();
    cell.setCellValue(input);
    assertThrows(IllegalArgumentException.class, () -> SpreadsheetHelper.getCellAsLocalDate(cell));
  }

  @Test
  public void getCellAsLocalDate_InValidNumericInput_ThrowsException() {
    Cell cell = createTestWorkbookCell();
    cell.setCellValue(5432);
    assertThrows(IllegalArgumentException.class, () -> SpreadsheetHelper.getCellAsLocalDate(cell));
  }

  @ParameterizedTest
  @ValueSource(strings = { "r0w", "2125", "col umn", "#%#&!" })
  public void getCellAsEnum_OnInvalidInput_ReturnsEmptyOptional(String input) {
    Cell cell = createTestWorkbookCell();
    cell.setCellValue(input);
    assertEquals(Optional.empty(), SpreadsheetHelper.getCellAsEnum(cell, TestEnum::fromString));
  }

  @Test
  public void getCellAsEnum_BlankAndNull_ReturnsEmptyOptional() {
    assertEquals(Optional.empty(), SpreadsheetHelper.getCellAsEnum(null, TestEnum::fromString));
    assertEquals(Optional.empty(), SpreadsheetHelper.getCellAsEnum(createTestWorkbookCell(), TestEnum::fromString));
  }

  @ParameterizedTest
  @ValueSource(strings = { "a", "value a" })
  public void getCellAsFillDirection_OnValidRowInput_ReturnOptionalFillDirectionByRow(String input) {
    Cell cell = createTestWorkbookCell();
    cell.setCellValue(input);
    assertEquals(TestEnum.A, SpreadsheetHelper.getCellAsEnum(cell, TestEnum::fromString).get());
  }

  @Test
  public void getCellAsEnum_OnValidEnum_ReturnOptionalEnum() {
    Cell cell = createTestWorkbookCell();
    cell.setCellValue("a");

    Optional<TestEnum> sampleType = SpreadsheetHelper.getCellAsEnum(cell, TestEnum.class);
    assertEquals(TestEnum.A, sampleType.get());
  }

  @Test
  public void getCellAsEnumWithFct_OnValidEnumWithFct_ReturnOptionalEnum() {
    Cell cell = createTestWorkbookCell();
    cell.setCellValue("value b");

    Optional<TestEnum> sampleType = SpreadsheetHelper.getCellAsEnum(cell,
        TestEnum::fromString);
    assertEquals(TestEnum.B, sampleType.get());
  }

  @ParameterizedTest
  @ValueSource(strings = { "abc" })
  @NullAndEmptySource
  public void getCellAsEnum_OnInvalidString_ReturnOptionalEmpty(String value) {
    Cell cell = createTestWorkbookCell();
    cell.setCellValue(value);
    assertEquals(Optional.empty(), SpreadsheetHelper.getCellAsEnum(cell, TestEnum.class));
  }


  private enum TestEnum {
    A,B;

    static Optional<TestEnum> fromString(String str) {
      if( str.equalsIgnoreCase("value a") || str.equalsIgnoreCase(A.name())) {
        return Optional.of(TestEnum.A);
      }
      else if( str.equalsIgnoreCase("value b") || str.equalsIgnoreCase(B.name())) {
        return Optional.of(TestEnum.B);
      }
      return Optional.empty();
    }
  }

}
