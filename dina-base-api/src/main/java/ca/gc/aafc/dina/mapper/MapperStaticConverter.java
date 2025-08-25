package ca.gc.aafc.dina.mapper;

import java.util.List;
import java.util.UUID;

import ca.gc.aafc.dina.dto.ExternalRelationDto;

/**
 * Usage:
 * On class @Mapper( imports = MapperStaticConverter.class)
 *
 * On method @Mapping(target = ..., expression = "java(MapperStaticConverter.uuidToExternalRelation(entity.getAgent(), \"person\"))")
 */
public final class MapperStaticConverter {


  private MapperStaticConverter() {
    // Utility class
  }

  public static ExternalRelationDto uuidToExternalRelation(UUID personUUID, String type) {
    if (personUUID == null) {
      return null;
    }
    return ExternalRelationDto.builder().id(personUUID.toString()).type(type).build();
  }

  public static List<ExternalRelationDto> uuidListToExternalRelationsList(List<UUID> metadataUUIDs,
                                                                          String type) {
    return metadataUUIDs == null ? null :
      metadataUUIDs.stream().map(uuid ->
        ExternalRelationDto.builder().id(uuid.toString()).type(type).build()).toList();
  }

}
