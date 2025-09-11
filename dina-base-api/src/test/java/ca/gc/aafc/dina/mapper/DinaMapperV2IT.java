package ca.gc.aafc.dina.mapper;

import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import ca.gc.aafc.dina.dto.ProjectDTO;
import ca.gc.aafc.dina.entity.Project;
import ca.gc.aafc.dina.entity.Task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class DinaMapperV2IT {

  @Test
  public void testMapper() {
    ProjectDtoMapper mapper = ProjectDtoMapper.INSTANCE;

    Project p = Project.builder()
      .name("my project")
      .task(Task.builder().uuid(UUID.randomUUID()).build())
      .build();

    ProjectDTO dto = mapper.toDto(p, Set.of("name", "task", "task.uuid"), null);
    assertEquals(p.getName(), dto.getName());
    assertEquals(p.getTask().getUuid(), dto.getTask().getUuid());

    Project p2 = Project.builder()
      .name("my project with no task")
      .build();
    ProjectDTO dto2 = mapper.toDto(p2, Set.of("name", "task", "task.uuid"), null);
    assertNull(dto2.getTask());
  }
}
