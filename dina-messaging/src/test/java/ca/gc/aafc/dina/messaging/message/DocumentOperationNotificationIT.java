package ca.gc.aafc.dina.messaging.message;

import org.junit.jupiter.api.Test;

import tools.jackson.core.JacksonException;

import ca.gc.aafc.dina.messaging.DinaMessage;
import ca.gc.aafc.dina.testsupport.TestResourceHelper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

public class DocumentOperationNotificationIT {

  @Test
  public void testSerDe() throws JacksonException {

    DocumentOperationNotification don = DocumentOperationNotification.builder()
      .documentId("abc")
      .documentType("material-sample")
      .operationType(DocumentOperationType.ADD)
      .build();

    String asJson = TestResourceHelper.OBJECT_MAPPER.writeValueAsString(don);

    DocumentOperationNotification don2 = TestResourceHelper.OBJECT_MAPPER.readValue(asJson, DocumentOperationNotification.class);
    assertEquals(don, don2);

    DinaMessage don3 = TestResourceHelper.OBJECT_MAPPER.readValue(asJson, DinaMessage.class);
    assertInstanceOf(DocumentOperationNotification.class, don3);
  }
}
