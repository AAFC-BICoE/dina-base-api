package ca.gc.aafc.dina.service;

import ca.gc.aafc.dina.BasePostgresItContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import javax.inject.Inject;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PostgresJsonbServiceIT extends BasePostgresItContext {

  @Inject
  private PostgresJsonbService postgresJsonbService;

  @DisplayName("Test countFirstLevelKeys when key exist in JSONb column")
  @Test
  public void countFirstLevelKeys_KeyExistInJson() {
    // Run a countFirstLevelKeys that would only work in postgres
    assertEquals(
      // attr_01 does exist in one record in jdata column
      Integer.valueOf(1),
      postgresJsonbService.countFirstLevelKeys(
        "dina_jsonb",
        "jdata",
        "attr_01"
      )
    );
  }

  @DisplayName("Test countFirstLevelKeys when key does not exist in JSONb column")
  @Test
  public void countFirstLevelKeys_KeyNotExistInJson() {
    // Run countFirstLevelKeys that would only work in postgres
    assertEquals(
      // attr_03 does not exist in any records in jdata column
      Integer.valueOf(0),
      postgresJsonbService.countFirstLevelKeys(
        "dina_jsonb",
        "jdata",
        "attr_03"
      )
    );
  }
}
