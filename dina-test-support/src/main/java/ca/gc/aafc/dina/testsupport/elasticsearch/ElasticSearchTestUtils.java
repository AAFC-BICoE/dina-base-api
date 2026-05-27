package ca.gc.aafc.dina.testsupport.elasticsearch;

import ca.gc.aafc.dina.testsupport.TestResourceHelper;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.Refresh;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.DeleteIndexRequest;
import co.elastic.clients.elasticsearch.indices.ExistsRequest;
import java.io.IOException;
import java.io.StringReader;

/**
 * Utility class to help with ElasticSearch testing.
 */
public final class ElasticSearchTestUtils {

  public enum ActionOnExists { DROP, IGNORE, FAIL }

  private ElasticSearchTestUtils() {
    // utility class
  }

  public static void createIndex(ElasticsearchClient client, String indexName,
                                 String mappingJsonFile) throws IOException {
    createIndex(client, indexName, mappingJsonFile, ActionOnExists.FAIL);
  }

  /**
   * Create an Index with a specific mapping
   * @param client ElasticSearch client
   * @param indexName name of the index
   * @param mappingJsonFile a file available on the classpath that contains the mapping required for the index
   * @param actionOnExists in case the index already exists, what should be done?
   * @throws IOException
   */
  public static void createIndex(ElasticsearchClient client, String indexName,
                                 String mappingJsonFile, ActionOnExists actionOnExists) throws IOException {
    String esSettings = TestResourceHelper
      .readContentAsString(mappingJsonFile);

    ExistsRequest e = ExistsRequest.of(b -> b.index(indexName));
    //Check if the index already exists
    if (client.indices().exists(e).value()) {
      if (ActionOnExists.IGNORE == actionOnExists) {
        return;
      } else if (ActionOnExists.DROP == actionOnExists) {
        DeleteIndexRequest dr = DeleteIndexRequest.of(b -> b.index(indexName));
        client.indices().delete(dr);
      } else if (ActionOnExists.FAIL == actionOnExists) {
        // noop, it will fail
      } else {
        throw new IllegalArgumentException();
      }
    }

    CreateIndexRequest createIndexRequest = CreateIndexRequest.of(
      b -> b.withJson(new StringReader(esSettings)).index(indexName)
    );
    client.indices().create(createIndexRequest);
  }

  /**
   * Index a document in ElasticSearch.
   * @param client ElasticSearch client
   * @param indexName name of the index
   * @param docId identifier of the document
   * @param jsonContent the content of the document as json
   */
  public static void indexDocument(ElasticsearchClient client, String indexName, String docId,
                                   String jsonContent) throws IOException {

    IndexRequest<Object> indexRequest = IndexRequest.of(b -> b
      .index(indexName)
      .id(docId)
      .withJson(new StringReader(jsonContent))
      // From ES documentation: This should ONLY be done after careful thought and verification that it does not lead to poor performance
      // For testing we can afford the performance hit since we need the document indexed to continue the test
      .refresh(Refresh.True)
    );
    client.index(indexRequest);
  }
}
