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
  public void test() {
    try (ElasticsearchContainer container = new ElasticsearchContainer(ElasticSearchTestUtils.ES_IMAGE)) {
      container.start();
      try {
        ElasticsearchClient client = ElasticSearchTestUtils.buildClient(container.getHost(), container.getFirstMappedPort());
        ElasticSearchTestUtils.createIndex(client, "agent_index", "elasticsearch/agent_schema.json");
      } catch (IOException e) {
        fail(e);
      }
    }
  }



}
