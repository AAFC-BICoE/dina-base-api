package ca.gc.aafc.dina.testsupport.elasticsearch;

import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.testcontainers.utility.DockerImageName;

import ca.gc.aafc.dina.testsupport.TestResourceHelper;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import java.io.IOException;
import java.io.StringReader;

public class ElasticSearchTestUtils {

  public static final DockerImageName ES_IMAGE =
    DockerImageName.parse("docker.elastic.co/elasticsearch/elasticsearch:7.17.10");

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

  public static void createIndex(ElasticsearchClient client, String indexName,
                                 String mappingJsonFile)
    throws IOException {
    String esSettings = TestResourceHelper
      .readContentAsString(mappingJsonFile);

    CreateIndexRequest createIndexRequest = CreateIndexRequest.of(
      map -> map.withJson(new StringReader(esSettings)).index(indexName)
    );
    client.indices().create(createIndexRequest);
  }

}
