package ca.gc.aafc.dina.datetime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * ISO DateTime representation with support for partial dates.
 * This class is immutable.
 *
 * localDateTime represents the earliest possible moment matching the input (e.g., "2004" → 2004-01-01T00:00:00),
 * while localEndDateTime represents the latest possible moment (e.g., "2004" → 2004-12-31T23:59:59.999999999).
 * Together, they define the full temporal range of a partial date, with both values being equal when the date is fully specified.
 */
@AllArgsConstructor
@Builder
@Getter
@EqualsAndHashCode
public class ISODateTime {
  
  private static final Pattern ALL_NON_NUMERIC = Pattern.compile("[^\\d]");

  private static final byte YYYY_PRECISION = 4;
  private static final byte YYYY_MM_PRECISION = 6;
  private static final byte YYYY_MM_DD_PRECISION = 8;
  private static final byte YYYY_MM_DD_HH_MM_PRECISION = 12;
  private static final byte YYYY_MM_DD_HH_MM_SS_PRECISION = 14;
  private static final byte YYYY_MM_DD_HH_MM_SS_MMM_PRECISION = 17;

  public enum Format {
    YYYY(YYYY_PRECISION),
    YYYY_MM(YYYY_MM_PRECISION),
    YYYY_MM_DD(YYYY_MM_DD_PRECISION),
    YYYY_MM_DD_HH_MM(YYYY_MM_DD_HH_MM_PRECISION),
    YYYY_MM_DD_HH_MM_SS(YYYY_MM_DD_HH_MM_SS_PRECISION),
    YYYY_MM_DD_HH_MM_SS_MMM(YYYY_MM_DD_HH_MM_SS_MMM_PRECISION);

    private final byte precision;
    Format(byte precision) {
      this.precision = precision;
    }

    /**
     * Tries to get a {@link Format} that is matching a precision representing the number
     * of numerical digits in an ISO date.
     *
     * @param precision
     * @return
     */
    public static Optional<Format> fromPrecision(int precision) {
      switch(precision) {
        case YYYY_PRECISION : return Optional.of(YYYY);
        case YYYY_MM_PRECISION : return Optional.of(YYYY_MM);
        case YYYY_MM_DD_PRECISION : return Optional.of(YYYY_MM_DD);
        case YYYY_MM_DD_HH_MM_PRECISION: return Optional.of(YYYY_MM_DD_HH_MM);
        case YYYY_MM_DD_HH_MM_SS_PRECISION: return Optional.of(YYYY_MM_DD_HH_MM_SS);
        default:
          break;
      }

      // Consider that all precision above seconds means milliseconds
      if (precision > YYYY_MM_DD_HH_MM_SS_PRECISION) {
        return Optional.of(YYYY_MM_DD_HH_MM_SS_MMM);
      }

      return Optional.empty();
    }

    public byte getPrecision() {
      return precision;
    }

  }

  private final LocalDateTime localDateTime;
  private final LocalDateTime localEndDateTime;
  private final Format format;

  /**
   * Tries to parse the provided date-time.
   *
   * @param dateTime date and time as string, can't be null
   * @return date time as {@link ISODateTime}
   * @throws DateTimeParseException if the provided value can't be parsed.
   */
  public static ISODateTime parse(@NonNull String dateTime) {
    int numberOfNumericChar = ALL_NON_NUMERIC
        .matcher(dateTime).replaceAll("").length();

    Format format = Format.fromPrecision(numberOfNumericChar)
        .orElseThrow(() -> new DateTimeParseException("unknown format", dateTime, 0));
    LocalDateTime parsedLocalDateTime = null;

    switch (format) {
      case YYYY:
        parsedLocalDateTime = Year.parse(dateTime).atDay(1).atStartOfDay();
        break;
      case YYYY_MM:
        parsedLocalDateTime = YearMonth.parse(dateTime).atDay(1).atStartOfDay();
        break;
      case YYYY_MM_DD:
        parsedLocalDateTime = LocalDate.parse(dateTime).atStartOfDay();
        break;
      case YYYY_MM_DD_HH_MM:
      case YYYY_MM_DD_HH_MM_SS:
      case YYYY_MM_DD_HH_MM_SS_MMM:
        parsedLocalDateTime = LocalDateTime.parse(dateTime);
        break;
      default:
        break;
    }
    return new ISODateTime(parsedLocalDateTime, calculateAmbiguityEndDateTime(parsedLocalDateTime, format), format);
  }

  /**
   * Calculates the end date-time of the ambiguity range of date/time based on the format precision.
   * <p>
   * This returns the latest possible moment that still matches
   * the original input. For fully specified dates, this returns the same value as the start.
   *
   * Precision is limited to milliseconds (3 decimal places) for database compatibility.
   * </p>
   *
   * <h3>Examples:</h3>
   * <ul>
   *   <li>YYYY (2004) → 2004-12-31T23:59:59.999999999</li>
   *   <li>YYYY_MM (2004-06) → 2004-06-30T23:59:59.999999999</li>
   *   <li>YYYY_MM_DD (2004-06-15) → 2004-06-15T23:59:59.999999999</li>
   *   <li>YYYY_MM_DD_HH_MM (2004-06-15T14:30) → 2004-06-15T14:30:59.999999999</li>
   *   <li>YYYY_MM_DD_HH_MM_SS (2004-06-15T14:30:45) → 2004-06-15T14:30:45.999999999</li>
   *   <li>YYYY_MM_DD_HH_MM_SS_MMM (2004-06-15T14:30:45.123) → 2004-06-15T14:30:45.123 (no change)</li>
   * </ul>
   *
   * @param start the start date-time (earliest possible interpretation)
   * @param format the precision format of the original input
   * @return the end date-time representing the latest possible moment in the ambiguous range
   * @throws IllegalStateException if an unexpected format is provided
   */
  private static LocalDateTime calculateAmbiguityEndDateTime(LocalDateTime start, Format format) {
    switch (format) {
      case YYYY:
        return YearMonth.from(start).withMonth(12).atEndOfMonth()
          .atTime(23, 59, 59, 999_000_000);  // 999 milliseconds
      case YYYY_MM:
        return YearMonth.from(start).atEndOfMonth()
          .atTime(23, 59, 59, 999_000_000);
      case YYYY_MM_DD:
        return start.toLocalDate().atTime(23, 59, 59, 999_000_000);
      case YYYY_MM_DD_HH_MM:
        return start.withSecond(59).withNano(999_000_000);
      case YYYY_MM_DD_HH_MM_SS:
        return start.withNano(999_000_000);
      case YYYY_MM_DD_HH_MM_SS_MMM:
        return start;
      default:
        throw new IllegalStateException("Unexpected format: " + format);
    }
  }

  public String toString() {
    switch (format) {
      case YYYY:
        return Year.from(localDateTime).toString();
      case YYYY_MM:
        return YearMonth.from(localDateTime).toString();
      case YYYY_MM_DD:
        return LocalDate.from(localDateTime).toString();
      case YYYY_MM_DD_HH_MM:
      case YYYY_MM_DD_HH_MM_SS:
      case YYYY_MM_DD_HH_MM_SS_MMM:
        return localDateTime.toString();
      default:
        throw new IllegalStateException("Unexpected format: " + format);
    }
  }
}
