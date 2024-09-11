package ca.gc.aafc.dina.mapper;

import org.junit.jupiter.api.Test;

import ca.gc.aafc.dina.dto.ChainDto;
import ca.gc.aafc.dina.entity.Chain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Set;
import java.util.UUID;


public class DtoMapperTest {

  @Test
  public void mapWithPartialSet() {

    ChainDto dto = new ChainDto();
    dto.setGroup("my group");
    dto.setName("my name");
    dto.setUuid(UUID.randomUUID());
    // set only the properties provided
    Chain chain = DtoMapper.INSTANCE.toChain(dto, Set.of("group"));

    assertEquals("my group", chain.getGroup());
    assertNull(chain.getName());
  }
}
