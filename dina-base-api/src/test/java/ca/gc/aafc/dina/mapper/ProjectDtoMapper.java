package ca.gc.aafc.dina.mapper;

import org.mapstruct.BeanMapping;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.factory.Mappers;

import ca.gc.aafc.dina.dto.ProjectDTO;
import ca.gc.aafc.dina.dto.TaskDTO;
import ca.gc.aafc.dina.entity.Project;
import ca.gc.aafc.dina.entity.Task;

import java.util.Set;

@Mapper
public interface ProjectDtoMapper extends DinaMapperV2<ProjectDTO, Project> {

  ProjectDtoMapper INSTANCE = Mappers.getMapper( ProjectDtoMapper.class );

  @Mapping(source = "acMetaDataCreator", target = "acMetaDataCreator", qualifiedByName = "uuidToPersonExternalRelation")
  @Mapping(source = "originalAuthor", target = "originalAuthor", qualifiedByName = "uuidToPersonExternalRelation")
  @Mapping(target = "randomPeople", ignore = true)
  @Mapping(target = "authors", ignore = true)
  ProjectDTO toDto(Project entity, @Context Set<String> provided, @Context String scope);

  @Mapping(target = "randomPeople", ignore = true)
  @Mapping(target = "nameTranslations", ignore = true)
  Project toEntity(ProjectDTO dto, @Context Set<String> provided, @Context String scope);

  @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
  @Mapping(target = "randomPeople", ignore = true)
  @Mapping(target = "nameTranslations", ignore = true)
  void patchEntity(@MappingTarget Project entity, ProjectDTO dto, @Context Set<String> provided, @Context String scope);

  default Task toEntity(TaskDTO dto, @Context Set<String> provided, @Context String scope) {
    return toTaskEntity(dto, provided, "task");
  }

  default TaskDTO toDto(Task dto, @Context Set<String> provided, @Context String scope) {
    return toTaskDto(dto, provided, "task");
  }

  // Relationships handling
  Task toTaskEntity(TaskDTO dto, Set<String> provided, String scope);
  TaskDTO toTaskDto(Task entity, Set<String> provided, String scope);

}
