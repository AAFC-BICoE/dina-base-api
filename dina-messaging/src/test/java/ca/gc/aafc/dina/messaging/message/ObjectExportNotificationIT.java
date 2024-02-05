package ca.gc.aafc.dina.messaging.message;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;

import ca.gc.aafc.dina.testsupport.TestResourceHelper;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ObjectExportNotificationIT {

  @Test
  public void testSerDe() throws JsonProcessingException {

    ObjectExportNotification oen = ObjectExportNotification.builder()
      .uuid(UUID.randomUUID())
      .username("user")
      .toa("toa").build();

    String asJson = TestResourceHelper.OBJECT_MAPPER.writeValueAsString(oen);

    ObjectExportNotification oen2 = TestResourceHelper.OBJECT_MAPPER.readValue(asJson, ObjectExportNotification.class);
    assertEquals(oen, oen2);
  }
}
