package ca.gc.aafc.dina.filter;

import java.time.OffsetDateTime;
import java.util.UUID;

import javax.inject.Named;

import com.github.tennaito.rsql.misc.DefaultArgumentParser;

import lombok.SneakyThrows;

/** Augments the RSQL DefaultArgumentParser with additional filterable types. */
@Named
public class DinaFilterArgumentParser extends DefaultArgumentParser {
  
  /** Override the default parse method to add more parseable types to filter by. */
  @SneakyThrows
  @SuppressWarnings("unchecked")
  public <T> T parse(String argument, Class<T> type) {
    // Handle null values before not-nul values:
    if (argument == null || "null".equals(argument.trim().toLowerCase())) {
      return (T) null;
    }

    if (type.equals(UUID.class)) {
      return (T) UUID.fromString(argument);
    }
    if (type.equals(OffsetDateTime.class)) {
      return (T) OffsetDateTime.parse(argument);
    }

    // Otherwise fallback to the default behavior:
    return super.parse(argument,type);
  }
  
}
