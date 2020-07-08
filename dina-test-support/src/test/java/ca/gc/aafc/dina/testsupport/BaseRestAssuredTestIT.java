package ca.gc.aafc.dina.testsupport;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Test making sure {@link BaseRestAssuredTest} is usable.
 */
@SpringBootTest(classes = TestConfiguration.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class BaseRestAssuredTestIT extends BaseRestAssuredTest {

  public BaseRestAssuredTestIT() {
    super("");
  }
    
  @Test
	public void testAccessHome() {
	  sendGet("crnk-test-data", UUID.randomUUID().toString(), 200);
	}
}