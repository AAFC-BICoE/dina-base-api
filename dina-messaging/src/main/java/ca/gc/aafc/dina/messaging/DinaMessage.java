package ca.gc.aafc.dina.messaging;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import ca.gc.aafc.dina.messaging.message.DocumentOperationNotification;
import ca.gc.aafc.dina.messaging.message.ObjectExportNotification;
import ca.gc.aafc.dina.messaging.message.ReportTemplateUploadNotification;

/**
 * Marker interface to identify messages in Dina
 */
@JsonTypeInfo(
  use = JsonTypeInfo.Id.NAME,
  include = JsonTypeInfo.As.PROPERTY,
  property = "type")
@JsonSubTypes({
  @JsonSubTypes.Type(value = DocumentOperationNotification.class, name = DocumentOperationNotification.TYPE),
  @JsonSubTypes.Type(value = ReportTemplateUploadNotification.class, name = ReportTemplateUploadNotification.TYPE),
  @JsonSubTypes.Type(value = ObjectExportNotification.class, name = ObjectExportNotification.TYPE)
})
public abstract class DinaMessage implements Serializable {

  private final String type;

  public DinaMessage(String type) {
    this.type = type;
  }

  public String getType() {
    return type;
  }
}
