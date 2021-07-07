package ca.gc.aafc.dina.workbook;

import java.sql.Date;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;

/**
 *
 * Methods returning primitive as object are returning Optional to prevent any unboxing issue. All
 * methods are null-safe to ease the usage with Apache POI methods that can return null on empty
 * {@link Cell}.
 *
 */
public final class SpreadsheetHelper {

  private SpreadsheetHelper() {
    // static utility class
  }

  /**
   * Check if row is empty.
   * https://stackoverflow.com/questions/12217047/how-to-determine-empty-row/28451783
   *
   * @param row
   *          the row
   * @return true, if the row is empty (row has only blank cells)
   */
  public static boolean checkIfRowIsEmpty(Row row) {
    if (row == null) {
      return true;
    }

    if (row.getLastCellNum() <= 0) {
      return true;
    }
    for (int cellNum = row.getFirstCellNum(); cellNum < row.getLastCellNum(); cellNum++) {
      Cell cell = row.getCell(cellNum);
      if (cell != null && cell.getCellType() != CellType.BLANK
          && StringUtils.isNotBlank(cell.toString())) {
        return false;
      }
    }
    return true;
  }

  /**
   * Get the content of a {@link Cell} as Boolean using BooleanUtils.toBooleanObject. If the cell type
   * is not a boolean, the String value of the cell is used to extract a boolean value.
   * If there is a "1" or "0", return true or false.
   * If a Boolean equivalent could not be found from a populated cell, a IllegalArgumentException will be thrown.
   *
   * @param cell
   *          the cell with either a boolean or string with acceptable values(@see
   *          BooleanUtils.toBooleanObject)
   * @return An Optional with the boolean value encoded in the cell or Optional.empty if the Cell is
   *         null or empty
   * @throws IllegalArgumentException
   *           if the cell value is not blank and toBooleanObject returned null e.g cell with value
   *           of "blue".
   */
  public static Optional<Boolean> getCellAsBoolean(Cell cell) {
    if (cell == null) {
      return Optional.empty();
    }

    if (cell.getCellType() == CellType.BOOLEAN) {
      return Optional.of(cell.getBooleanCellValue());
    } else {
      String value = getCellAsString(cell);
      if (StringUtils.isBlank(value)) {
        return Optional.empty();
      }

      if ("1".equals(value) || "0".equals(value)) {
        value = String.valueOf(BooleanUtils.toBoolean(Integer.parseInt(value)));
      }

      Boolean result = BooleanUtils.toBooleanObject(value);
      if (result == null) {
        throw new IllegalArgumentException("'" + getCellAsString(cell)
            + "' could not parse boolean value from the cell's content");
      }

      return Optional.of(result);
    }
  }

  /**
   * Returns a {@link Date} or null if the cell is empty. Accepts ISO 8601
   * (YYYY-MM-DD) string formats and Excel internal date representations.
   *
   * @param cell cell to read.
   * @throws IllegalArgumentException if the cells value cannot be parsed as a
   *                                  date.
   * @return {@link Date} or null if the cell is empty.
   */
  public static Date getCellAsDate(Cell cell) {
    LocalDate cellAsLocalDate = getCellAsLocalDate(cell);
    return cellAsLocalDate == null ? null : Date.valueOf(cellAsLocalDate);
  }

  /**
   * Returns a {@link LocalDate} or null if the cell is empty. Accepts ISO 8601
   * (YYYY-MM-DD) string formats and Excel internal date representations.
   *
   * @param cell cell to read.
   * @throws IllegalArgumentException if the cells value cannot be parsed as a
   *                                  date.
   * @return {@link LocalDate} or null if the cell is empty.
   */
  public static LocalDate getCellAsLocalDate(Cell cell) {
    String cellAsString = getCellAsString(cell);

    if (StringUtils.isBlank(cellAsString)) {
      return null;
    }

    try {
      if (cell.getCellType() == CellType.STRING) {
        return LocalDate.parse(cellAsString);
      } else if (DateUtil.isCellDateFormatted(cell)) {
        return cell.getLocalDateTimeCellValue().toLocalDate();
      } else {
        throw new IllegalArgumentException(
            "'" + cellAsString + "' can not be parsed as yyyy-mm-dd");
      }
    } catch (DateTimeParseException | NumberFormatException ex) {
      throw new IllegalArgumentException(
          "'" + cellAsString + "' can not be parsed as yyyy-mm-dd", ex);
    }
  }

  /**
   * Get the content of a {@link Cell} as Integer.
   *
   * @param cell
   *          the {@link Cell} object with the integer value as numeric or string.
   * @return Optional containing the integer value of the cell content or Optional.empty()
   * @throws NumberFormatException
   *           if the cell value doesn't represent an integer
   */
  public static Optional<Integer> getCellAsInteger(Cell cell) {
    if (cell == null) {
      return Optional.empty();
    }
    String value = getCellAsString(cell);
    if (StringUtils.isBlank(value)) {
      return Optional.empty();
    }
    return Optional.of(Integer.valueOf(value));
  }

  /**
   * Get the content of a {@link Cell} as Double.
   *
   * @param cell
   *          the {@link Cell} object with the content as numeric or string.
   * @return Optional with the double value of the cell contents or Optional.empty()
   * @throws NumberFormatException
   *           if the cell value doesn't represent a double
   */
  public static Optional<Double> getCellAsDouble(Cell cell) {
    if (cell == null) {
      return Optional.empty();
    }
    String value = getCellAsString(cell);
    if (StringUtils.isBlank(value)) {
      return Optional.empty();
    }
    return Optional.of(Double.parseDouble(value));

  }

  /**
   * Get the content of a {@link Cell} as Float.
   *
   * @param cell
   *          the {@link Cell} object with the content as numeric or string.
   * @return Optional with the float value of the cell contents or Optional.empty()
   * @throws NumberFormatException
   *           if the cell value doens't represent a double
   */
  public static Optional<Float> getCellAsFloat(Cell cell) {
    if (cell == null) {
      return Optional.empty();
    }
    String value = getCellAsString(cell);
    if (StringUtils.isBlank(value)) {
      return Optional.empty();
    }
    return Optional.of(Float.parseFloat(value));

  }


  /**
   * Returns a string representing the content of the cell. Trims the value before returning it.
   * If the cell type is FORMULA, get the result type of the equation. Else, get the type of the cell.
   * If the type of the cell is no STRING, BLANK, BOOLEAN or NUMERIC, null is returned.
   * @param cell
   *          a cell from spreadsheet
   * @return string representing the content of the cell or null
   */
  public static String getCellAsString(Cell cell) {

    if (cell == null) {
      return null;
    }

    String value = null;

    CellType effectiveCellType = cell.getCellType() == CellType.FORMULA ? cell.getCachedFormulaResultType() : cell.getCellType();
    switch (effectiveCellType) {
        case STRING:
          value = cell.getStringCellValue();
          break;
        case BLANK:
          value = null;
          break;
        case BOOLEAN:
          value = String.valueOf(cell.getBooleanCellValue());
          break;
        case NUMERIC:
          double n = cell.getNumericCellValue();
          if (Math.ceil(n) == Math.floor(n)) {
            value = String.valueOf(Math.round(n));
          } else {
            value = String.valueOf(n);
          }
          break;
        default:
          break; //noop, continue
    }

    if (value != null) {
      value = value.trim();
    }

    return value;
  }

  /**
   * Get the value of a cell as a specific enum. The match is case insensitive and will be done one
   * the name() function of the enum. It is also possible to give a specific method to use for the
   * parsing using {@link #getCellAsEnum(Cell, Class, Function)}.
   *
   * @param cell
   * @param enumClass
   *          the class of the enum (not null)
   * @return
   */
  public static <E extends Enum<E>> Optional<E> getCellAsEnum(Cell cell, Class<E> enumClass) {
    Objects.requireNonNull(enumClass);
    return getCellAsEnum(cell, enumClass, null);
  }

  /**
   * Get the value of a cell as a specific enum using a specific parsing function that could be more
   * lenient. The function is usually a fromString on the enum class.
   *
   * @param cell
   * @param fromStringFct
   *          the parsing function (not null)
   * @return
   */
  public static <E extends Enum<E>> Optional<E> getCellAsEnum(Cell cell,
      Function<String, Optional<E>> fromStringFct) {
    Objects.requireNonNull(fromStringFct);
    return getCellAsEnum(cell, null, fromStringFct);
  }

  /**
   * Internal function for enum parsing.
   *
   * @param cell
   * @param enumClass
   * @param fromStringFct
   * @return
   */
  private static <E extends Enum<E>> Optional<E> getCellAsEnum(Cell cell, Class<E> enumClass,
      Function<String, Optional<E>> fromStringFct) {

    if (cell == null) {
      return Optional.empty();
    }
    String value = getCellAsString(cell);
    if (StringUtils.isBlank(value)) {
      return Optional.empty();
    }

    // if you have a specific function fromString just use it
    if (fromStringFct != null) {
      return fromStringFct.apply(value);
    }

    value = value.toLowerCase();
    for (E val : enumClass.getEnumConstants()) {
      if (val.name().toLowerCase().equalsIgnoreCase(value)) {
        return Optional.of(val);
      }
    }
    return Optional.empty();
  }

}

