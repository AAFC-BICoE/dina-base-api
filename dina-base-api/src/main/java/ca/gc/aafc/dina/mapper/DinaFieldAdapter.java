package ca.gc.aafc.dina.mapper;

import java.util.function.Consumer;

public interface DinaFieldAdapter<DTO, ENTITY, DTO_FIELD, ENTITY_FIELD> {

  DTO_FIELD toDTO(ENTITY entityRef);

  ENTITY_FIELD toEntity(DTO dtoRef);

  Consumer<ENTITY_FIELD> entityApplyMethod(ENTITY entityRef);

  Consumer<DTO_FIELD> dtoApplyMethod(DTO dtoRef);

}
