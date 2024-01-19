package ca.gc.aafc.dina.messaging.message;

import lombok.Builder;
import lombok.Getter;

import ca.gc.aafc.dina.messaging.DinaMessage;

/**
 * Class representing a message to indicate a change on a document (JSON document)
 *
 */
@Getter
public class DocumentOperationNotification implements DinaMessage {

  public static final String NOT_DEFINED = "Not-Defined";

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

  /**
   * Document operation notification.
   *
   * @param dryRun flag denoting if the operation/processing associated with the message should be
   * bypassed.
   * @param documentType DINA document type (metadata, person, organization, etc...)
   * @param documentId The document UUID
   * @param operationType Operation type as defined by the enumerated type.
   */
  @Builder
  public DocumentOperationNotification(boolean dryRun, String documentType, String documentId,
                                       DocumentOperationType operationType) {
    this.dryRun = dryRun;
    this.documentId = documentId;
    this.documentType = documentType;
    this.operationType = operationType;
  }

  @Override
  public String toString() {
    return "DocumentOperationNotification [operationType=" + operationType + ", documentId=" + documentId
      + ", documentType=" + documentType + ", dryRun=" + dryRun + "]";
  }
}
