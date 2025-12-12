package ca.gc.aafc.dina.search.config;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import lombok.extern.log4j.Log4j2;

@Configuration
@Log4j2
public class ElasticSearchConfig {

  public static final int DEFAULT_PORT = 9200;

  @Bean
  public ElasticsearchClient provideClient(ElasticSearchProperties esProps) throws Exception {
    int port = esProps.getPort() <= 0 ? DEFAULT_PORT : esProps.getPort();

    log.debug("Configuring Elasticsearch client for {}:{}", esProps.getHost(), port);

    var restClientBuilder = RestClient.builder(
      new HttpHost(esProps.getHost(), port)
    );

    // Optional
    CredentialsProvider credentialsProvider = null;
    SSLContext sslContext = null;

    if (StringUtils.isNotBlank(esProps.getUsername())) {
      credentialsProvider = new BasicCredentialsProvider();
      credentialsProvider.setCredentials(AuthScope.ANY,
        new UsernamePasswordCredentials(esProps.getUsername(), esProps.getPassword()));
    }

    if (StringUtils.isNotBlank(esProps.getCertPath())) {
      sslContext = createSSLContext(esProps.getCertPath());
    }
    return createElasticsearchClient(restClientBuilder, credentialsProvider, sslContext);
  }

  public static ElasticsearchClient createElasticsearchClient(RestClientBuilder restClientBuilder,
                                                       CredentialsProvider credentialsProvider,
                                                       SSLContext sslContext) {
    RestClient restClient =
      restClientBuilder
        .setHttpClientConfigCallback(httpClientBuilder -> {
          if (credentialsProvider != null) {
            httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
          }
          if (sslContext != null) {
            httpClientBuilder.setSSLContext(sslContext);
          }
          return httpClientBuilder;
        })
        .build();

    return new ElasticsearchClient(
      new RestClientTransport(restClient, new JacksonJsonpMapper())
    );
  }

  private SSLContext createSSLContext(String certPath) throws Exception {
    File certFile = new File(certPath);
    if (!certFile.exists()) {
      throw new FileNotFoundException("Certificate file not found: " + certFile.getAbsolutePath());
    }

    try (FileInputStream fis = new FileInputStream(certFile)) {
      CertificateFactory cf = CertificateFactory.getInstance("X.509");
      X509Certificate cert = (X509Certificate) cf.generateCertificate(fis);

      KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
      keyStore.load(null, null);
      keyStore.setCertificateEntry("elasticsearch", cert);

      TrustManagerFactory tmf = TrustManagerFactory
        .getInstance(TrustManagerFactory.getDefaultAlgorithm());
      tmf.init(keyStore);

      SSLContext sslContext = SSLContext.getInstance("TLS");
      sslContext.init(null, tmf.getTrustManagers(), new SecureRandom());

      log.debug("✅ SSL Context loaded successfully");
      return sslContext;
    }
  }
}
