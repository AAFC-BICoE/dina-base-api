package ca.gc.aafc.dina.mapper;

import io.crnk.core.queryspec.FilterSpec;
import lombok.Builder;
import lombok.Data;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

class DinaFieldAdapterHandlerTest {

  private static final DinaFieldAdapterHandler<CarDto> handler =
    new DinaFieldAdapterHandler<>(CarDto.class);

  @Test
  void resolveFields_ToDto_FieldsResolved() {
    Car entity = Car.builder()
      .customField("1")
      .build();
    CarDto dto = CarDto.builder().build();
    handler.resolveFields(entity, dto);
    Assertions.assertEquals(Integer.valueOf(entity.getCustomField()), dto.getCustomField());
  }

  @Test
  void resolveFields_ToEntity_FieldsResolved() {
    CarDto dto = CarDto.builder()
      .customField(1)
      .build();
    Car entity = Car.builder().build();
    handler.resolveFields(dto, entity);
    Assertions.assertEquals(Integer.toString(dto.getCustomField()), entity.getCustomField());
  }

  @Builder
  @Data
  public static class Car {
    String name;
    int powerLevel;
    String customField;

    public void applyCustomField(String value) {
      this.customField = value;
    }
  }

  @Builder
  @Data
  @CustomFieldAdapter(adapters = CustomFieldAdapterImpl.class)
  public static class CarDto {
    String name;
    int powerLevel;
    int customField;

    public void applyCustomField(Integer value) {
      this.customField = value;
    }
  }

  static class CustomFieldAdapterImpl implements DinaFieldAdapter<CarDto, Car, Integer, String> {

    //No args constructor required
    public CustomFieldAdapterImpl() {
    }

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

    @Override
    public Map<String, Function<Object, FilterSpec[]>> toFilterSpec(Object value) {
      return Map.of();
    }

    @Override
    public Supplier<String> entitySupplyMethod(Car entityRef) {
      return entityRef::getCustomField;
    }

    @Override
    public Supplier<Integer> dtoSupplyMethod(CarDto dtoRef) {
      return dtoRef::getCustomField;
    }
  }

}
