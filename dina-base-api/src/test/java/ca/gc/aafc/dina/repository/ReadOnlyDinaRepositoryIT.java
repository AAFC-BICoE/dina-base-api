package ca.gc.aafc.dina.repository;

import ca.gc.aafc.dina.TestConfiguration;
import ca.gc.aafc.dina.dto.VocabularyDto;
import ca.gc.aafc.dina.entity.Vocabulary;
import ca.gc.aafc.dina.jpa.BaseDAO;
import io.crnk.core.exception.MethodNotAllowedException;
import io.crnk.core.queryspec.QuerySpec;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.inject.Inject;
import javax.transaction.Transactional;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Transactional
@SpringBootTest(classes = TestConfiguration.class)
public class ReadOnlyDinaRepositoryIT {

  @Inject
  private ReadOnlyDinaRepository<VocabularyDto, Vocabulary> dinaRepository;

  @Inject
  private BaseDAO baseDAO;

  @Test
  public void create_onValidResource_OperationFails() {
    VocabularyDto vocabularyDto = VocabularyDto.builder().name("test create vocab").build();
    assertThrows(
        MethodNotAllowedException.class,
        ()-> dinaRepository.create(vocabularyDto)
    );
  }

  @Test
  public void findOne_onResourceAvailable_ResourceFound() {
    Vocabulary vocabulary = Vocabulary.builder().uuid(UUID.randomUUID()).name("test find vocab").build();
    baseDAO.create(vocabulary);
    VocabularyDto vocabDto = dinaRepository.findOne(vocabulary.getUuid(), new QuerySpec(VocabularyDto.class));

    assertEquals(vocabulary.getName(), vocabDto.getName());
  }

  @Test
  public void findAll_onResourcesAvailable_ResourcesFound() {
    Vocabulary vocabulary = Vocabulary.builder().uuid(UUID.randomUUID()).name("test find all vocab 1").build();
    baseDAO.create(vocabulary);
    Vocabulary vocabulary2 = Vocabulary.builder().uuid(UUID.randomUUID()).name("test find all vocab 2").build();
    baseDAO.create(vocabulary2);

    List<VocabularyDto> result = dinaRepository.findAll(Arrays.asList(vocabulary.getUuid(), vocabulary2.getUuid()), new QuerySpec(VocabularyDto.class));

    assertEquals(2, result.size());
  }

  @Test
  public void delete_onResourceAvailable_DeleteFails() {
    Vocabulary vocabulary = Vocabulary.builder().uuid(UUID.randomUUID()).name("test delete vocab").build();
    baseDAO.create(vocabulary);

    assertThrows(
        MethodNotAllowedException.class,
        ()-> dinaRepository.delete(vocabulary.getUuid())
    );
  }

  @Test
  public void save_onResourceAvailable_UpdateFails() {
    Vocabulary vocabulary = Vocabulary.builder().uuid(UUID.randomUUID()).name("test update vocab").build();
    baseDAO.create(vocabulary);

    VocabularyDto vocabDto = dinaRepository.findOne(vocabulary.getUuid(), new QuerySpec(VocabularyDto.class));
    vocabDto.setName("new updated name");

    assertThrows(
        MethodNotAllowedException.class,
        ()-> dinaRepository.save(vocabDto)
    );
  }

}
