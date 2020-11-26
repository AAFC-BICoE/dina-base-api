package ca.gc.aafc.dina.mapper;

import lombok.Builder;
import lombok.Data;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.org.apache.commons.lang.RandomStringUtils;

import java.util.Set;

class DinaFieldAdapterHandlerTest {

  private static final DinaFieldAdapterHandler handler = new DinaFieldAdapterHandler();

  @Test
  void resolveFields_ToDto_FieldsResolved() {
    Car entity = Car.builder()
      .name(RandomStringUtils.randomAlphabetic(5))
      .powerLevel(9000)
      .build();
    CarDto dto = CarDto.builder().build();
    handler.resolveFields(Set.of("customField"), entity, dto);
    Assertions.assertEquals(entity.getPowerLevel(), dto.getCustomField());
  }

  @Test
  void resolveFields_ToEntity_FieldsResolved() {
    CarDto dto = CarDto.builder()
      .name(RandomStringUtils.randomAlphabetic(5))
      .powerLevel(9000)
      .build();
    Car entity = Car.builder().build();
    handler.resolveFields(Set.of("customField"), dto, entity);
    Assertions.assertEquals(dto.getName(), entity.getCustomField());
  }

  @Builder
  @Data
  static class Car {
    String name;
    int powerLevel;
    // uses dto name
    String customField;
  }

  @Builder
  @Data
  static class CarDto {
    String name;
    int powerLevel;
    // uses entity power level
    int customField;
  }

}
