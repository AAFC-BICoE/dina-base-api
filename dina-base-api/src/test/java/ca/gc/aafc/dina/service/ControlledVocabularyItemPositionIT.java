package ca.gc.aafc.dina.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import ca.gc.aafc.dina.TestDinaBaseApp;
import ca.gc.aafc.dina.entity.ControlledVocabulary;
import ca.gc.aafc.dina.entity.MyControlledVocabulary;
import ca.gc.aafc.dina.entity.MyControlledVocabularyItem;
import ca.gc.aafc.dina.testsupport.DatabaseSupportService;
import ca.gc.aafc.dina.testsupport.PostgresTestContainerInitializer;
import ca.gc.aafc.dina.vocabulary.TypedVocabularyElement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.UUID;
import jakarta.inject.Inject;

/**
 * Not transactional on purpose: the position uniqueness constraint is deferred so it is only
 * checked on commit.
 */
@SpringBootTest(classes = TestDinaBaseApp.class)
@ContextConfiguration(initializers = { PostgresTestContainerInitializer.class })
@Import(ControlledVocabularyServiceIT.MyControlledVocabularyServiceTestConfig.class)
public class ControlledVocabularyItemPositionIT {

  @Inject
  private ControlledVocabularyService<MyControlledVocabulary> controlledVocabularyService;

  @Inject
  private ControlledVocabularyItemService<MyControlledVocabularyItem> controlledVocabularyItemService;

  @Inject
  private PlatformTransactionManager transactionManager;

  @Inject
  private DatabaseSupportService dbSupport;

  @AfterEach
  public void cleanUp() {
    dbSupport.runInNewTransaction(em -> {
      em.createQuery("delete from test_controlled_vocabulary_item").executeUpdate();
      em.createQuery("delete from test_controlled_vocabulary").executeUpdate();
    });
  }

  @Test
  public void orderedVocabulary_insertAndMoveItem_uniquenessCheckedOnCommit() {
    TransactionTemplate tx = new TransactionTemplate(transactionManager);

    MyControlledVocabulary vocab = tx.execute(status -> controlledVocabularyService.create(
      MyControlledVocabulary.builder()
        .uuid(UUID.randomUUID())
        .type(ControlledVocabulary.ControlledVocabularyType.SYSTEM)
        .vocabClass(ControlledVocabulary.ControlledVocabularyClass.CONTROLLED_TERM)
        .name("ordered vocab")
        .ordered(true)
        .createdBy("test").build()));

    // 2 items at positions 1 and 2
    UUID item1 = createItem(tx, vocab, "item 1", 1);
    createItem(tx, vocab, "item 2", 2);

    // new item at position 3
    UUID item3 = createItem(tx, vocab, "item 3", 3);

    // moving the last item to position 2 alone conflicts with item 2, rejected on commit
    assertThrows(DataIntegrityViolationException.class, () -> setPosition(tx, item3, 2));
    assertEquals(3, findPosition(tx, item3));

    // moving the last item to position 2 and the previous one to position 3 is valid
    UUID item2 = tx.execute(status ->
      controlledVocabularyItemService.findOneByKey("item_2", vocab.getUuid()).getUuid());
    tx.executeWithoutResult(status -> {
      updatePosition(item3, 2);
      updatePosition(item2, 3);
    });

    assertEquals(1, findPosition(tx, item1));
    assertEquals(2, findPosition(tx, item3));
    assertEquals(3, findPosition(tx, item2));
  }

  @Test
  public void orderedVocabulary_duplicatedPosition_rejectedOnCommit() {
    TransactionTemplate tx = new TransactionTemplate(transactionManager);

    MyControlledVocabulary vocab = tx.execute(status -> controlledVocabularyService.create(
      MyControlledVocabulary.builder()
        .uuid(UUID.randomUUID())
        .type(ControlledVocabulary.ControlledVocabularyType.SYSTEM)
        .vocabClass(ControlledVocabulary.ControlledVocabularyClass.CONTROLLED_TERM)
        .name("ordered vocab")
        .ordered(true)
        .createdBy("test").build()));

    createItem(tx, vocab, "item 1", 1);

    // a second item at the same position is rejected by the database when committing
    assertThrows(DataIntegrityViolationException.class, () -> createItem(tx, vocab, "item 2", 1));
  }

  private UUID createItem(TransactionTemplate tx, MyControlledVocabulary vocab, String name, int position) {
    return tx.execute(status -> controlledVocabularyItemService.create(
      MyControlledVocabularyItem.builder()
        .uuid(UUID.randomUUID())
        .group("grp")
        .name(name)
        .vocabularyElementType(TypedVocabularyElement.VocabularyElementType.STRING)
        .position(position)
        .createdBy("test")
        .controlledVocabulary(vocab).build()).getUuid());
  }

  private void setPosition(TransactionTemplate tx, UUID itemUuid, int position) {
    tx.executeWithoutResult(status -> updatePosition(itemUuid, position));
  }

  private void updatePosition(UUID itemUuid, int position) {
    MyControlledVocabularyItem item =
      controlledVocabularyItemService.findOne(itemUuid, MyControlledVocabularyItem.class);
    item.setPosition(position);
    controlledVocabularyItemService.update(item);
  }

  private int findPosition(TransactionTemplate tx, UUID itemUuid) {
    return tx.execute(status ->
      controlledVocabularyItemService.findOne(itemUuid, MyControlledVocabularyItem.class).getPosition());
  }
}
