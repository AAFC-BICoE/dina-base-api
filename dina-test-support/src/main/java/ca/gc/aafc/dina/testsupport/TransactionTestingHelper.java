package ca.gc.aafc.dina.testsupport;

import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import javax.inject.Inject;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Helper class to run testing methods in their own transactions and commit.
 * Only use when the transaction should absolutely be committed in order to create a valid test.
 */
@Service
public class TransactionTestingHelper {

  @Inject
  private TransactionTemplate txTemplate;

  public <T> T doInTransaction(Supplier<T> operation) {
    return txTemplate.execute(status -> operation.get());
  }

  public void doInTransactionWithoutResult(Consumer<TransactionStatus> operation) {
    txTemplate.executeWithoutResult(operation);
  }
}
