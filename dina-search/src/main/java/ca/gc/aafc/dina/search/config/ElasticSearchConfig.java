package ca.gc.aafc.dina.search.config;

import org.apache.commons.lang3.StringUtils;
import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.CredentialsProvider;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.core5.http.HttpHost;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.Jackson3JsonpMapper;
import co.elastic.clients.transport.rest5_client.Rest5ClientTransport;
import co.elastic.clients.transport.rest5_client.low_level.Rest5Client;
import co.elastic.clients.transport.rest5_client.low_level.Rest5ClientBuilder;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import lombok.extern.log4j.Log4j2;

@ConditionalOnProperty("elasticsearch.host")
@Configuration
@Log4j2
public class ElasticSearchConfig {

  public static final int DEFAULT_PORT = 9200;

  @Bean
  public ElasticsearchClient provideClient(ElasticSearchProperties esProps) throws IOException {
    int port = esProps.getPort() <= 0 ? DEFAULT_PORT : esProps.getPort();

    HttpHost host = new HttpHost(esProps.getScheme(), esProps.getHost(), port);

    log.debug("Configuring Elasticsearch client for {}", host);

    var restClientBuilder = Rest5Client.builder(host);

    SSLContext sslContext = null;
    if (StringUtils.isNotBlank(esProps.getCertPath())) {
      sslContext = createSSLContext(esProps.getCertPath());
    }
    return createElasticsearchClient(restClientBuilder,
      createCredentialsProvider(host, esProps.getUsername(), esProps.getPassword()), sslContext);
  }

  private static CredentialsProvider createCredentialsProvider(HttpHost host, String username, String password) {
    if (StringUtils.isBlank(username)) {
      return null;
    }
    BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
    credentialsProvider.setCredentials(new AuthScope(host),
      new UsernamePasswordCredentials(username, password == null ? null : password.toCharArray()));
    return credentialsProvider;
  }

  /**
   * Allow to create an {@link ElasticsearchClient} programmatically with a specific SSLContext
   * @param restClientBuilder
   * @param credentialsProvider
   * @param sslContext
   * @return
   */
  public static ElasticsearchClient createElasticsearchClient(Rest5ClientBuilder restClientBuilder,
                                                       CredentialsProvider credentialsProvider,
                                                       SSLContext sslContext) {
    if (sslContext != null) {
      restClientBuilder.setSSLContext(sslContext);
    }
    Rest5Client restClient =
      restClientBuilder
        .setHttpClientConfigCallback(httpClientBuilder -> {
          if (credentialsProvider != null) {
            httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
          }
        })
        .build();

    return new ElasticsearchClient(
      new Rest5ClientTransport(restClient, new Jackson3JsonpMapper())
    );
  }

  private SSLContext createSSLContext(String certPath) throws IOException {
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
    } catch (CertificateException | KeyStoreException | NoSuchAlgorithmException |
             KeyManagementException e) {
      throw new RuntimeException(e);
    }
  }
}
