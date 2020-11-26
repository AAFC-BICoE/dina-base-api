package ca.gc.aafc.dina.mapper;

import lombok.Builder;
import org.junit.jupiter.api.Test;

class DinaFieldAdapterHandlerTest {

  @Test
  void resolveFields_FieldsResolved() {

  }

  @Builder
  static class Car {
    String name;
    int powerLevel;
  }

  @Builder
  static class CarDto {
    String name;
    int powerLevel;
  }

}