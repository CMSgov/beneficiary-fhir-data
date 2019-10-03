package gov.cms.bfd.server.test.perf.utils;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import org.apache.http.HttpHost;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.log4j.BasicConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;

/**
 * A class that simplifies connecting to FHIR backend server(s) that require a secure
 * connection(X509).
 */
public final class FhirClient {
  private static final Logger LOGGER = LoggerFactory.getLogger(FhirClient.class);

  /**
   * @param fhirServerUrlText a {@link String} for the URL of the FHIR server to create a client for
   * @param clientKeystoreDirText a {@link String} path to the client keystore directory
   * @return a new FHIR {@link IGenericClient} for use
   */
  public static IGenericClient create(String fhirServerUrlText, String clientKeystoreDirText) {
    return FhirClient.create(fhirServerUrlText, clientKeystoreDirText, null, 0);
  }

  /**
   * @param fhirServerUrlText a {@link String} for the URL of the FHIR server to create a client for
   * @param clientKeystoreDirText a {@link String} path to the client keystore directory
   * @param proxyHost a {@link String} to a proxy hostname to use for the client connection
   * @param proxyPort a {@link int} to the proxy port to use for the client connection
   * @return a new FHIR {@link IGenericClient} for use
   */
  @SuppressWarnings("deprecation")
  public static IGenericClient create(
      String fhirServerUrlText, String clientKeystoreDirText, String proxyHost, int proxyPort) {

    final char[] JKS_PASSWORD = "changeit".toCharArray();
    final char[] KEY_PASSWORD = "changeit".toCharArray();

    BasicConfigurator.configure();

    FhirContext ctx = FhirContext.forDstu3();

    /*
     * The default timeout is 10s, which was failing for batches of 100. A 300s
     * timeout was failing for batches of 100 once Part B claims were mostly mapped,
     * so batches were cut to 10, which ran at 12s or so, each.
     */
    ctx.getRestfulClientFactory().setSocketTimeout(300 * 1000);

    /*
     * We need to override the FHIR client's SSLContext. Unfortunately, that
     * requires overriding the entire HttpClient that it uses. Otherwise, the
     * settings used here mirror those that the default FHIR HttpClient would use.
     */
    try {
      // Load the java keystore
      final KeyStore keystore = KeyStore.getInstance("JKS");
      try (final InputStream is =
          new FileInputStream(
              getClientKeyStorePath(clientKeystoreDirText, "client.keystore").toFile())) {
        keystore.load(is, JKS_PASSWORD);
      }
      final KeyManagerFactory kmf =
          KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
      kmf.init(keystore, KEY_PASSWORD);

      // create an SSLContext
      final SSLContext sslContext = SSLContext.getInstance("TLS");

      // create a TrustManager that trusts all hosts
      TrustManager tm =
          new X509TrustManager() {
            public void checkClientTrusted(X509Certificate[] chain, String authType)
                throws CertificateException {}

            public void checkServerTrusted(X509Certificate[] chain, String authType)
                throws CertificateException {}

            public X509Certificate[] getAcceptedIssuers() {
              return null;
            }
          };

      // initialize the ssl context
      sslContext.init(kmf.getKeyManagers(), new TrustManager[] {tm}, new SecureRandom());
      SSLConnectionSocketFactory ssl = new SSLConnectionSocketFactory(sslContext, NoopHostnameVerifier.INSTANCE);


      // create a http client connection manager
      PoolingHttpClientConnectionManager connectionManager =
          new PoolingHttpClientConnectionManager(
              RegistryBuilder.<ConnectionSocketFactory>create()
                  .register("http", PlainConnectionSocketFactory.getSocketFactory())
                  .register("https", ssl)
                  .build(),
              null,
              null,
              null,
              5000,
              TimeUnit.MILLISECONDS);

      // defaults to 2 concurrent and 20 max that will not be enough
      // https://hc.apache.org/httpcomponents-client-ga/httpclient/apidocs/org/apache/http/impl/conn/PoolingHttpClientConnectionManager.html
      // Tweaked settings to perform well on a laptop running 10 threads with
      // 1000 thread loop count
      connectionManager.setDefaultMaxPerRoute(100);
      connectionManager.setMaxTotal(1000);

      // configure connection properties
      RequestConfig defaultRequestConfig;
      if (proxyHost != null) { // configuration with proxy
        defaultRequestConfig =
            RequestConfig.custom()
                .setSocketTimeout(ctx.getRestfulClientFactory().getSocketTimeout())
                .setConnectTimeout(ctx.getRestfulClientFactory().getConnectTimeout())
                .setConnectionRequestTimeout(
                    ctx.getRestfulClientFactory().getConnectionRequestTimeout())
                .setProxy(new HttpHost(proxyHost, proxyPort))
                .setStaleConnectionCheckEnabled(true)
                .build();
        LOGGER.info("Using proxy " + proxyHost + ":" + proxyPort);
      } else { // configuration without proxy
        defaultRequestConfig =
            RequestConfig.custom()
                .setSocketTimeout(ctx.getRestfulClientFactory().getSocketTimeout())
                .setConnectTimeout(ctx.getRestfulClientFactory().getConnectTimeout())
                .setConnectionRequestTimeout(
                    ctx.getRestfulClientFactory().getConnectionRequestTimeout())
                .setStaleConnectionCheckEnabled(true)
                .build();
        LOGGER.info("Direct connection, no proxy");
      }

      // create http client
      HttpClient httpClient =
          HttpClients.custom()
              .setConnectionManager(connectionManager)
              .setDefaultRequestConfig(defaultRequestConfig)
              .disableCookieManagement()
              .build();
      ctx.getRestfulClientFactory().setHttpClient(httpClient);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    } catch (KeyManagementException
        | UnrecoverableKeyException
        | NoSuchAlgorithmException
        | KeyStoreException
        | CertificateException e) {
      throw new IllegalStateException(e);
    }

    // create a restful client
    IGenericClient client = ctx.newRestfulGenericClient(fhirServerUrlText);
    return client;
  }

  /**
   * @param storeDir a {@link String} for the path to the key store
   * @param storeName a {@link String} for the filename of the key store
   * @return the local {@link Path} to the key store that FHIR clients should use
   */
  private static Path getClientKeyStorePath(String storeDir, String storeName)
      throws IllegalStateException {
    /*
     * The working directory for tests will either be the module directory or their
     * parent directory. With that knowledge, we're searching for the ssl-stores
     * directory.
     */
    Path sslStoresDir = Paths.get(storeDir);
    if (!Files.isDirectory(sslStoresDir)) throw new IllegalStateException();

    Path keyStorePath = sslStoresDir.resolve(storeName);
    LOGGER.debug("keyStorePath = " + keyStorePath.toString());
    return keyStorePath;
  }
}
