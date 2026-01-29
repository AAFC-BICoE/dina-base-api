package ca.gc.aafc.dina.messaging.message;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import com.fasterxml.jackson.annotation.JsonTypeId;

import ca.gc.aafc.dina.messaging.DinaMessage;

/**
 * Class representing a message to indicate a change on a document (JSON document)
 *
 */
@Data
@AllArgsConstructor
@Builder
public class DocumentOperationNotification implements DinaMessage {

  public static final String TYPE = "DocumentOperationNotification";
  public static final String NOT_DEFINED = "Not-Defined";

  @JsonTypeId
  private String type = TYPE;

  private final boolean dryRun;
  private final String documentId;
  private final String documentType;
  private final DocumentOperationType operationType;

  public DocumentOperationNotification() {
    this.dryRun = false;
    this.documentId = NOT_DEFINED;
    this.documentType = NOT_DEFINED;
    this.operationType = DocumentOperationType.NOT_DEFINED;
  }

  @Override
  public String getType() {
    return TYPE;
  }

  @Override
  public String toString() {
    return "DocumentOperationNotification [operationType=" + operationType + ", documentId=" + documentId
      + ", documentType=" + documentType + ", dryRun=" + dryRun + "]";
  }
}
