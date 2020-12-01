package ca.gc.aafc.dina.mapper;

import java.util.function.Consumer;
import java.util.function.Supplier;

public interface DinaFieldAdapter<DTO, ENTITY, DTO_FIELD, ENTITY_FIELD> {

  DTO_FIELD toDTO(ENTITY_FIELD entityField);

  ENTITY_FIELD toEntity(DTO_FIELD dtoField);

  Consumer<ENTITY_FIELD> entityApplyMethod(ENTITY entityRef);

  Consumer<DTO_FIELD> dtoApplyMethod(DTO dtoRef);

  Supplier<ENTITY_FIELD> entitySupplyMethod(ENTITY entityRef);

  Supplier<DTO_FIELD> dtoSupplMethod(DTO dtoRef);

}
