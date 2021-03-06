package ca.gc.aafc.dina.mapper;

import io.crnk.core.queryspec.FilterSpec;

import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public interface DinaFieldAdapter<DTO, ENTITY, DTO_FIELD, ENTITY_FIELD> {

  DTO_FIELD toDTO(ENTITY_FIELD entityField);

  ENTITY_FIELD toEntity(DTO_FIELD dtoField);

  Consumer<ENTITY_FIELD> entityApplyMethod(ENTITY entityRef);

  Consumer<DTO_FIELD> dtoApplyMethod(DTO dtoRef);

  Supplier<ENTITY_FIELD> entitySupplyMethod(ENTITY entityRef);

  Supplier<DTO_FIELD> dtoSupplyMethod(DTO dtoRef);

  /**
   * Returns a map of supported field names to their supporting filter spec mapping function.
   *
   * @return a map of supported field names to their supporting filter spec mapping function.
   */
  default Map<String, Function<FilterSpec, FilterSpec[]>> toFilterSpec() {
    return Map.of();
  }

}
