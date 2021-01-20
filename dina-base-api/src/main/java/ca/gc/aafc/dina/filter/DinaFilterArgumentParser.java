package ca.gc.aafc.dina.filter;

import com.github.tennaito.rsql.misc.DefaultArgumentParser;
import lombok.SneakyThrows;

import javax.inject.Named;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Augments the RSQL DefaultArgumentParser with additional filterable types. */
@Named
public class DinaFilterArgumentParser extends DefaultArgumentParser {
  
  /** Override the default parse method to add more parseable types to filter by. */
  @SneakyThrows
  @SuppressWarnings("unchecked")
  public <T> T parse(String argument, Class<T> type) {
    // Handle null values before not-nul values:
    if (argument == null || "null".equalsIgnoreCase(argument.trim())) {
      return null;
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

    // Otherwise fallback to the default behavior:
    return super.parse(argument,type);
  }
  
}
