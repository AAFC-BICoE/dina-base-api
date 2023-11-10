package ca.gc.aafc.dina.mapper;

import java.util.Map;

import org.junit.jupiter.api.Test;

import ca.gc.aafc.dina.dto.ChainDto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


public class DtoMapperTest {

  @Test
  public void a() {
    ChainDto chain = DtoMapper.INSTANCE.toChainDto(Map.of("group", "grp1"));
    assertEquals("grp1", chain.getGroup());
  }
}
