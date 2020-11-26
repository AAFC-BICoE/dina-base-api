package ca.gc.aafc.dina.mapper;

import lombok.Builder;
import lombok.Data;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.function.Consumer;

class DinaFieldAdapterHandlerTest {

  private static final DinaFieldAdapterHandler handler = new DinaFieldAdapterHandler();

  @Test
  void resolveFields_ToDto_FieldsResolved() {
    Car entity = Car.builder()
      .customField("1")
      .build();
    CarDto dto = CarDto.builder().build();
    handler.resolveFields(Set.of("customField"), entity, dto);
    Assertions.assertEquals(Integer.valueOf(entity.getCustomField()), dto.getCustomField());
  }

  @Test
  void resolveFields_ToEntity_FieldsResolved() {
    CarDto dto = CarDto.builder()
      .customField(1)
      .build();
    Car entity = Car.builder().build();
    handler.resolveFields(Set.of("customField"), dto, entity);
    Assertions.assertEquals(Integer.toString(dto.getCustomField()), entity.getCustomField());
  }

  @Builder
  @Data
  static class Car {
    String name;
    int powerLevel;
    String customField;

    public void applyCustomField(String value) {
      this.customField = value;
    }
  }

  @Builder
  @Data
  static class CarDto {
    String name;
    int powerLevel;

    @CustomFieldAdapter(adapter = CustomFieldAdapterImpl.class)
    int customField;

    public void applyCustomField(Integer value) {
      this.customField = value;
    }

  }

  static class CustomFieldAdapterImpl implements DinaFieldAdapter<CarDto, Car, Integer, String> {

    @Override
    public Integer toDTO(String s) {
      return Integer.valueOf(s);
    }

    @Override
    public String toEntity(Integer integer) {
      return Integer.toString(integer);
    }

    @Override
    public Consumer<String> entityApplyMethod(Car dtoRef) {
      return dtoRef::applyCustomField;
    }

    @Override
    public Consumer<Integer> dtoApplyMethod(CarDto entityRef) {
      return entityRef::applyCustomField;
    }
  }

}
