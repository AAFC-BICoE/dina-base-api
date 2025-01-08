package ca.gc.aafc.dina.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import ca.gc.aafc.dina.dto.TaskDTO;
import ca.gc.aafc.dina.entity.Task;

import java.util.Map;

@Mapper
public interface TaskDtoMapper extends DinaMapperV2<TaskDTO, Task> {

  TaskDtoMapper INSTANCE = Mappers.getMapper(TaskDtoMapper.class);

  @Mapping(target = "powerLevel", ignore = true)
  @Mapping(target = "uuid", source = "id")
  TaskDTO toTaskDto(Map<String, String> map);
}
