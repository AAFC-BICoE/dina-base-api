package ca.gc.aafc.dina.mapper;

import org.junit.jupiter.api.Test;

import ca.gc.aafc.dina.dto.ChainDto;
import ca.gc.aafc.dina.entity.Chain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Set;
import java.util.UUID;


public class ChainMapperTest {

  @Test
  public void mapWithPartialSet() {

    ChainDto dto = new ChainDto();
    dto.setGroup("my group");
    dto.setName("my name");
    dto.setUuid(UUID.randomUUID());
    // set only the properties provided
    Chain chain = ChainMapper.INSTANCE.toEntity(dto, Set.of("group"), null);

    assertEquals("my group", chain.getGroup());
    assertNull(chain.getName());

    Chain entity = new Chain();
    entity.setGroup("my group");
    entity.setName("my name");
    entity.setUuid(UUID.randomUUID());
    // set only the properties provided
    ChainDto chainDto = ChainMapper.INSTANCE.toDto(entity, Set.of("group"), null);

    assertEquals("my group", chainDto.getGroup());
    assertNull(chainDto.getName());
  }
}
