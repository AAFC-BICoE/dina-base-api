package ca.gc.aafc.dina.testsupport.elasticsearch;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.testcontainers.elasticsearch.ElasticsearchContainer;

import static org.junit.jupiter.api.Assertions.fail;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import java.io.IOException;

@SpringBootTest
public class ElasticSearchTestUtilsIT {

  @Autowired
  protected RestTemplateBuilder builder;

  @Test
  public void onCreateIndexAndIndexDocument_success() {
    try (ElasticsearchContainer container = new ElasticsearchContainer(ElasticSearchTestUtils.ES_IMAGE)) {
      container.start();
      try {
        ElasticsearchClient client = ElasticSearchTestUtils.buildClient(container.getHost(), container.getFirstMappedPort());
        ElasticSearchTestUtils.createIndex(client, "agent_index", "elasticsearch/agent_schema.json");

        String jsonDocument = """
          {
            "data": {
              "id": "9df388de-71b5-45be-9613-b70674439773",
              "type": "person",
              "attributes": {
                "displayName": "test user"
              }
            }
          }""";
        ElasticSearchTestUtils.indexDocument(client, "agent_index", "9df388de-71b5-45be-9613-b70674439773", jsonDocument);
      } catch (IOException e) {
        fail(e);
      }
    }
  }



}
