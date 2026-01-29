package ca.gc.aafc.dina.messaging.message;


import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;

import ca.gc.aafc.dina.testsupport.TestResourceHelper;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class UserMessageNotificationIT {

  @Test
  public void testSerDe() throws JsonProcessingException {

    UserMessageNotification oen = UserMessageNotification.builder()
      .title("notification").build();

    String asJson = TestResourceHelper.OBJECT_MAPPER.writeValueAsString(oen);

    UserMessageNotification oen2 = TestResourceHelper.OBJECT_MAPPER.readValue(asJson, UserMessageNotification.class);
    assertEquals(oen, oen2);
  }
}
