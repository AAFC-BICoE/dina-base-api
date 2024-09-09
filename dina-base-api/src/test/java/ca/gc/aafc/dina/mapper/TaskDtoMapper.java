package ca.gc.aafc.dina.mapper;

import java.util.Map;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import ca.gc.aafc.dina.dto.TaskDTO;

@Mapper
public interface TaskDtoMapper {

  TaskDtoMapper INSTANCE = Mappers.getMapper( TaskDtoMapper.class );

  @Mapping(target = "powerLevel", ignore = true)
  @Mapping(target = "uuid", source = "id")
  TaskDTO toTaskDto(Map<String, String> map);

}
