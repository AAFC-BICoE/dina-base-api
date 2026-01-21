package ca.gc.aafc.dina.testsupport.elasticsearch;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;

import ca.gc.aafc.dina.search.config.ElasticSearchConfig;
import ca.gc.aafc.dina.search.config.ElasticSearchProperties;

import static org.junit.jupiter.api.Assertions.fail;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import java.io.IOException;

@SpringBootTest()
@ContextConfiguration(initializers = { ElasticSearchContainerInitializer.class })
@Import({ElasticSearchConfig.class, ElasticSearchProperties.class})
public class ElasticSearchTestUtilsIT {

  @Autowired
  protected ElasticsearchClient esClient;

  @Test
  public void onCreateIndexAndIndexDocument_success() {

    try {
      ElasticSearchTestUtils.createIndex(esClient, "agent_index",
        "elasticsearch/agent_schema.json");

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
      ElasticSearchTestUtils.indexDocument(esClient, "agent_index",
        "9df388de-71b5-45be-9613-b70674439773", jsonDocument);
    } catch (IOException e) {
      fail(e);
    }
  }
}
