package ca.gc.aafc.dina.search.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;

import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ElasticSearchConfig {

  public static final int DEFAULT_PORT = 9200;

  @Bean
  public ElasticsearchClient provideClient(ElasticSearchProperties esProps) {
    int port = esProps.getPort() <= 0 ? DEFAULT_PORT : esProps.getPort();
    RestClient restClient = RestClient.builder(
      new HttpHost(esProps.getHost(), port)
    ).build();

    // Create the elastic search transport using Jackson and the low level rest client.
    ElasticsearchTransport transport = new RestClientTransport(
      restClient, new JacksonJsonpMapper());

    // Create the elastic search client.
    return new ElasticsearchClient(transport);
  }
}
