package ca.gc.aafc.dina.messaging.message;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.exc.InvalidTypeIdException;

import ca.gc.aafc.dina.messaging.DinaMessage;
import ca.gc.aafc.dina.testsupport.TestResourceHelper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Testing serialization/deserialization of DinaMessage with polymorphism.
 */
public class DinaMessageSerDeIT {

  @Test
  public void testSerDe() throws JsonProcessingException {

    ObjectExportNotification oen = ObjectExportNotification.builder()
      .uuid(UUID.randomUUID())
      .username("user")
      .toa("toa").build();

    ReportTemplateUploadNotification rtun = ReportTemplateUploadNotification.builder()
      .uuid(UUID.randomUUID())
      .username("user")
      .toa("toa").build();

    String oenAsJson = TestResourceHelper.OBJECT_MAPPER.writeValueAsString(oen);
    String rtunAsJson = TestResourceHelper.OBJECT_MAPPER.writeValueAsString(rtun);

    DinaMessage oenAsMessage = TestResourceHelper.OBJECT_MAPPER.readValue(oenAsJson, DinaMessage.class);
    assertEquals(ObjectExportNotification.class, oenAsMessage.getClass());

    DinaMessage rtunAsMessage = TestResourceHelper.OBJECT_MAPPER.readValue(rtunAsJson, DinaMessage.class);
    assertEquals(ReportTemplateUploadNotification.class, rtunAsMessage.getClass());

    assertThrows(InvalidTypeIdException.class, () -> TestResourceHelper.OBJECT_MAPPER.readValue(rtunAsJson, ObjectExportNotification.class));
  }
}
