package ca.gc.aafc.dina.filter;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.UUID;

/**
 * Parses filter arguments (String) into the Java type of the targeted attribute.
 *
 * The default conversions (String, numbers, boolean, enum, Date and valueOf(String) fallback)
 * are adapted from the DefaultArgumentParser of rsql-jpa (MIT License)
 * https://github.com/tennaito/rsql-jpa
 */
public class DinaFilterArgumentParser {

  private static final String DATE_PATTERN = "yyyy-MM-dd";
  private static final String DATE_TIME_PATTERN = "yyyy-MM-dd'T'HH:mm:ss";

  /**
   * Parse the given argument into the provided type.
   *
   * @param argument the argument to parse. null or "null" (case-insensitive) will return null.
   * @param type the targeted type
   * @return the parsed value or null
   * @throws IllegalArgumentException if the argument can't be parsed or the type is not supported
   */
  @SuppressWarnings({"unchecked", "rawtypes"})
  public <T> T parse(String argument, Class<T> type) {
    // Handle null values before not-null values:
    if (argument == null || "null".equalsIgnoreCase(argument.trim())) {
      return null;
    }

    try {
      if (type.equals(String.class)) {
        return (T) argument;
      }
      if (type.equals(UUID.class)) {
        return (T) UUID.fromString(argument);
      }
      if (type.equals(OffsetDateTime.class)) {
        return (T) OffsetDateTime.parse(argument);
      }
      if (type.equals(LocalDateTime.class)) {
        return (T) LocalDateTime.parse(argument);
      }
      if (type.equals(Integer.class) || type.equals(int.class)) {
        return (T) Integer.valueOf(argument);
      }
      if (type.equals(Boolean.class) || type.equals(boolean.class)) {
        return (T) Boolean.valueOf(argument);
      }
      if (type.isEnum()) {
        return (T) Enum.valueOf((Class<Enum>) type, argument);
      }
      if (type.equals(Float.class) || type.equals(float.class)) {
        return (T) Float.valueOf(argument);
      }
      if (type.equals(Double.class) || type.equals(double.class)) {
        return (T) Double.valueOf(argument);
      }
      if (type.equals(Long.class) || type.equals(long.class)) {
        return (T) Long.valueOf(argument);
      }
      if (type.equals(BigDecimal.class)) {
        return (T) new BigDecimal(argument);
      }
    } catch (IllegalArgumentException | DateTimeParseException ex) {
      throw formatException(argument, type, ex);
    }

    if (type.equals(Date.class)) {
      return (T) parseDate(argument);
    }

    // Fallback: try to parse using a static valueOf(String) method
    try {
      Method method = type.getMethod("valueOf", String.class);
      return (T) method.invoke(type, argument);
    } catch (InvocationTargetException ex) {
      throw formatException(argument, type, ex.getCause());
    } catch (NoSuchMethodException | IllegalAccessException ex) {
      throw new IllegalArgumentException("Cannot parse argument type " + type, ex);
    }
  }

  private static Date parseDate(String argument) {
    try {
      return new SimpleDateFormat(DATE_TIME_PATTERN).parse(argument);
    } catch (ParseException ex) {
      // not a date-time, try with date only
    }
    try {
      return new SimpleDateFormat(DATE_PATTERN).parse(argument);
    } catch (ParseException ex) {
      throw formatException(argument, Date.class, ex);
    }
  }

  private static IllegalArgumentException formatException(String argument, Class<?> type,
                                                          Throwable cause) {
    return new IllegalArgumentException(
      "Cannot parse argument '" + argument + "' as type " + type.getSimpleName(), cause);
  }
}
