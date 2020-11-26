package ca.gc.aafc.dina.mapper;

import java.util.function.Consumer;

public interface DinaFieldAdapter<DTO, ENTITY, DTO_FIELD, ENTITY_FIELD> {

  DTO_FIELD toDTO(ENTITY_FIELD entityField);

  ENTITY_FIELD toEntity(DTO_FIELD dtoField);

  Consumer<ENTITY_FIELD> entityApplyMethod(DTO dtoRef);

  Consumer<DTO_FIELD> dtoApplyMethod(ENTITY entityRef);

}
