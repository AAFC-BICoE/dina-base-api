package ca.gc.aafc.dina.testsupport.elasticsearch;

import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.testcontainers.utility.DockerImageName;

import ca.gc.aafc.dina.testsupport.TestResourceHelper;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.Refresh;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import java.io.IOException;
import java.io.StringReader;

/**
 * Utility class to help with ElasticSearch testing.
 */
public final class ElasticSearchTestUtils {

  public static final DockerImageName ES_IMAGE =
    DockerImageName.parse("docker.elastic.co/elasticsearch/elasticsearch:7.17.10");

  private ElasticSearchTestUtils() {
    // utility class
  }

  public static ElasticsearchClient buildClient(String host, int port) {
    RestClient restClient = RestClient.builder(
      new HttpHost(host, port)
    ).build();

    // Create the elastic search transport using Jackson and the low level rest client.
    ElasticsearchTransport transport = new RestClientTransport(
      restClient, new JacksonJsonpMapper());

    // Create the elastic search client.
    return new ElasticsearchClient(transport);
  }

  /**
   * Create an Index with a specific mapping
   * @param client ElasticSearch client
   * @param indexName name of the index
   * @param mappingJsonFile a file available on the classpath that contains the mapping required for the index
   * @throws IOException
   */
  public static void createIndex(ElasticsearchClient client, String indexName,
                                 String mappingJsonFile) throws IOException {
    String esSettings = TestResourceHelper
      .readContentAsString(mappingJsonFile);

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
