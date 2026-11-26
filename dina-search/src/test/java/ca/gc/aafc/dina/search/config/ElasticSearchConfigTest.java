package ca.gc.aafc.dina.search.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.transport.rest5_client.Rest5ClientTransport;
import java.util.List;

class ElasticSearchConfigTest {

  private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
    .withConfiguration(AutoConfigurations.of(ConfigurationPropertiesAutoConfiguration.class))
    .withUserConfiguration(ElasticSearchConfig.class, ElasticSearchProperties.class);

  @Test
  void noClientWithoutElasticsearchHost() {
    contextRunner.run(context -> assertThat(context).doesNotHaveBean(ElasticsearchClient.class));
  }

  @Test
  void springElasticsearchPropertiesAreIgnored() {
    contextRunner
      .withPropertyValues("spring.elasticsearch.uris=http://spring-host:9201")
      .run(context -> assertThat(context).doesNotHaveBean(ElasticsearchClient.class));
  }

  @Test
  void clientUsesLegacyHost() {
    contextRunner
      .withPropertyValues("elasticsearch.host=legacy-host", "elasticsearch.port=9200",
        "elasticsearch.scheme=http", "spring.elasticsearch.uris=http://spring-host:9201")
      .run(context -> assertThat(nodeHosts(context.getBean(ElasticsearchClient.class)))
        .containsExactly("http://legacy-host:9200"));
  }

  private static List<String> nodeHosts(ElasticsearchClient client) {
    return ((Rest5ClientTransport) client._transport()).restClient().getNodes().stream()
      .map(node -> node.getHost().toURI())
      .toList();
  }
}
