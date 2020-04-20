package ca.gc.aafc.dina.repository;

import io.crnk.core.engine.document.ErrorData;
import io.crnk.core.exception.CrnkMappableException;

/**
 * Crnk Exception for HTTP 410 : GONE
 *
 */
public class GoneException extends CrnkMappableException {

  private static final long serialVersionUID = 2658981047986565140L;

  public GoneException(String title, String message) {
    super(410, ErrorData.builder()
        .setTitle(title)
        .setDetail(message)
        .setStatus("410")
        .build());
  }

}
