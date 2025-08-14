package ca.gc.aafc.dina.search.helper;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.Time;
import co.elastic.clients.elasticsearch.core.ClosePointInTimeRequest;
import co.elastic.clients.elasticsearch.core.ClosePointInTimeResponse;
import co.elastic.clients.elasticsearch.core.OpenPointInTimeResponse;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import java.io.IOException;
import java.util.List;

/**
 * Utility functions for Elasticsearch Client.
 */
public final class ESClientHelper {

  private static final Time KEEP_ALIVE = new Time.Builder().time("60s").build();

  private ESClientHelper () {
    //utility class
  }

  /**
   * Open an ElasticSearch Point-in-time to go through multiple pages.
   * <a href="https://www.elastic.co/guide/en/elasticsearch/reference/7.17/paginate-search-results.html#search-after">https://www.elastic.co/guide/en/elasticsearch/reference/7.17/paginate-search-results.html#search-after</a>
   *
   */
  public static String openPointInTime(ElasticsearchClient client, String indexName) throws
    IOException {
    // create the PIT
    OpenPointInTimeResponse opitResponse =
      client.openPointInTime(b -> b.index(indexName).keepAlive(KEEP_ALIVE));
    return opitResponse.id();
  }

  public static String openPointInTime(ElasticsearchClient client, List<String> indices) throws
    IOException {
    // create the PIT
    OpenPointInTimeResponse opitResponse =
      client.openPointInTime(b -> b.index(indices).keepAlive(KEEP_ALIVE));
    return opitResponse.id();
  }

  public static SearchRequest.Builder setPitIdOnBuilder(SearchRequest.Builder builder,
                                                        String pitId) {
    return builder.pit(pit -> pit.id(pitId).keepAlive(KEEP_ALIVE));
  }

  /**
   * Close a previously opened PIT.
   * @param pitId
   * @return
   */
  public static boolean closePointInTime(ElasticsearchClient client, String pitId) throws IOException {
    ClosePointInTimeRequest request = ClosePointInTimeRequest.of(b -> b
      .id(pitId));
    ClosePointInTimeResponse csr = client.closePointInTime(request);
    return csr.succeeded();
  }
}
