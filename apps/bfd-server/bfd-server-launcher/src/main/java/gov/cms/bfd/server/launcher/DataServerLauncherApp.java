package gov.cms.bfd.server.launcher;

import ch.qos.logback.classic.LoggerContext;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Slf4jReporter;
import com.codahale.metrics.jvm.GarbageCollectorMetricSet;
import com.codahale.metrics.jvm.MemoryUsageGaugeSet;
import gov.cms.bfd.server.sharedutils.BfdMDC;
import gov.cms.bfd.sharedutils.config.ConfigException;
import gov.cms.bfd.sharedutils.config.ConfigLoader;
import gov.cms.bfd.sharedutils.config.ConfigLoaderSource;
import gov.cms.bfd.sharedutils.config.LayeredConfiguration;
import java.lang.Thread.UncaughtExceptionHandler;
import java.net.URI;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.eclipse.jetty.ee10.annotations.AnnotationConfiguration;
import org.eclipse.jetty.ee10.servlet.security.ConstraintMapping;
import org.eclipse.jetty.ee10.servlet.security.ConstraintSecurityHandler;
import org.eclipse.jetty.ee10.webapp.Configuration;
import org.eclipse.jetty.ee10.webapp.FragmentConfiguration;
import org.eclipse.jetty.ee10.webapp.JettyWebXmlConfiguration;
import org.eclipse.jetty.ee10.webapp.MetaInfConfiguration;
import org.eclipse.jetty.ee10.webapp.WebAppConfiguration;
import org.eclipse.jetty.ee10.webapp.WebAppContext;
import org.eclipse.jetty.ee10.webapp.WebInfConfiguration;
import org.eclipse.jetty.ee10.webapp.WebXmlConfiguration;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.security.Constraint;
import org.eclipse.jetty.security.authentication.SslClientCertAuthenticator;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.CustomRequestLog;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.Slf4jRequestLogWriter;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.util.resource.PathResourceFactory;
import org.eclipse.jetty.util.resource.ResourceFactory;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

/**
 * A "runner" for the Data Server WAR.
 *
 * <p>Basically, just a repackaging of the Jetty runner, with most of the configuration hardcoded,
 * to simplify deployment.
 */
public final class DataServerLauncherApp {
  /** The log message that will be fired once Jetty has been started. */
  static final String LOG_MESSAGE_STARTED_JETTY = "Started Jetty.";

  /**
   * The log message that will be fired at the end of the application's "housekeeping" shutdown
   * hook.
   */
  static final String LOG_MESSAGE_SHUTDOWN_HOOK_COMPLETE =
      "Application shutdown housekeeping complete.";

  private static final Logger LOGGER = LoggerFactory.getLogger(DataServerLauncherApp.class);

  /**
   * Logger for the structured access log ('access.json') that has one line for every request that
   * the server receives.
   */
  private static final Logger LOGGER_HTTP_ACCESS = LoggerFactory.getLogger("HTTP_ACCESS");

  /**
   * This {@link System#exit(int)} value should be used when the provided configuration values are
   * incomplete and/or invalid.
   */
  static final int EXIT_CODE_BAD_CONFIG = 1;

  /**
   * This {@link System#exit(int)} value should be used when the application exits due to an
   * unhandled exception.
   */
  static final int EXIT_CODE_MONITOR_ERROR = 2;

  /** Holds important information about the running jetty server and its web application. */
  @Getter
  @AllArgsConstructor
  public static class ServerInfo {
    /** The server object can be used to start and stop the server. */
    private Server server;

    /** The app context can be used to set attributes for use by the application. */
    private WebAppContext webapp;
  }

  /**
   * Creates a new {@link Server} and {@link WebAppContext} and returns them. Does not actually
   * start the server.
   *
   * @param appConfig used to configure the server
   * @return an object containing the values
   */
  public static ServerInfo createServer(AppConfiguration appConfig) {
    Server server = new Server(appConfig.getPort());

    // Modify the default HTTP config.
    HttpConfiguration httpConfig = new HttpConfiguration();
    httpConfig.setSecurePort(appConfig.getPort());

    // Create the HTTPS config.
    HttpConfiguration httpsConfig = new HttpConfiguration(httpConfig);
    SecureRequestCustomizer customizer = new SecureRequestCustomizer();
    /*
     * SNI (server name indication) is a TLS extension that allows a client to indicate
     * the server name (domain) it is issuing a request for which is helpful when multiple
     * domains are hosted at the same IP address. This indication is available before TLS
     * handshaking occurs which gives the server an opportunity to present a different
     * certificate for each server name (domain) that is being hosted. BFD only hosts one
     * domain (per environment) and only has one certificate. Therefore, SNI is unnecessary
     * for its intended purpose for BFD. Note as well that SNI is not a security mechanism --
     * it merely allows clients to indicate which domain they are trying to reach so that the
     * correct certificate will be returned from the server to prove its legitimacy to the
     * client. SNI does not influence the way that the server validates client certificates
     * or any other aspects of TLS.
     *
     * By default, SNI is not required by Jetty and BFD does not override that. However,
     * if SNI is provided by the client, Jetty 10 will, by default, check that the host
     * passed matches a certificate that is available to the server. This is a change from
     * Jetty 9 which did not perform this SNI validation. The server startup sanity checking
     * scripts and test tools make use of the ability to issue requests to localhost even
     * though no certificate exists for that host within BFD so in order to allow those
     * scripts to continue to work with Jetty 10, we turn off the Jetty SNI host name
     * checking here.
     */
    customizer.setSniHostCheck(false);
    httpsConfig.addCustomizer(customizer);

    // Create the SslContextFactory to be used, along with the cert.
    ResourceFactory pathResourceFactory = new PathResourceFactory();
    SslContextFactory.Server sslContextFactory = new SslContextFactory.Server();
    sslContextFactory.setNeedClientAuth(true);
    sslContextFactory.setKeyStoreResource(pathResourceFactory.newResource(appConfig.getKeystore()));
    sslContextFactory.setKeyStoreType("PKCS12");
    sslContextFactory.setKeyStorePassword("changeit");
    sslContextFactory.setCertAlias("server");
    sslContextFactory.setKeyManagerPassword("changeit");
    sslContextFactory.setTrustStoreResource(
        pathResourceFactory.newResource(appConfig.getTruststore()));
    sslContextFactory.setTrustStorePassword("changeit");
    sslContextFactory.setTrustStoreType("PKCS12");
    sslContextFactory.setExcludeProtocols(
        "SSL", "SSLv2", "SSLv2Hello", "SSLv3", "TLSv1", "TLSv1.1");
    sslContextFactory.setIncludeCipherSuites(
        "TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384",
        "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384",
        "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256",
        "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256",
        "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA384",
        "TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA384",
        "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256",
        "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256");

    // Apply the config.
    try (ServerConnector serverConnector =
        new ServerConnector(
            server,
            new SslConnectionFactory(sslContextFactory, HttpVersion.HTTP_1_1.toString()),
            new HttpConnectionFactory(httpsConfig))) {
      serverConnector.setHost(appConfig.getHost().orElse("0.0.0.0"));
      serverConnector.setPort(appConfig.getPort());
      server.setConnectors(new Connector[] {serverConnector});

      // Jetty will be used to run a WAR, via this WebAppContext.
      WebAppContext webapp = new WebAppContext();
      webapp.setContextPath("/");
      webapp.setWar(appConfig.getWar().toString());

      // Ensure that Jetty finds config via annotations, in addition to the usual.
      webapp.setConfigurations(
          new Configuration[] {
            new WebInfConfiguration(),
            new WebXmlConfiguration(),
            new WebAppConfiguration(),
            new MetaInfConfiguration(),
            new FragmentConfiguration(),
            new JettyWebXmlConfiguration(),
            new AnnotationConfiguration()
          });

      // Allow webapps to see but not override SLF4J (prevents LinkageErrors).
      webapp.getProtectedClassMatcher().add("org.slf4j.");

      /*
       * Disable Logback's builtin shutdown hook, so that OUR shutdown hook can still use the loggers
       * (and then shut Logback down itself).
       */
      webapp.setInitParameter("logbackDisableServletContainerInitializer", "true");

      /* Configure the log output generation via a Jetty CustomRequestLog.
       * As of Feb 8th 2023, the Access.log file has been removed, and BFD server is only writing to access.json.
       * CustomRequestLog allows with minimal side effects to get the response output size, so a blank writer and format
       * are instantiated to access the methods to get the response output size.
       *
       */
      final String requestLogFormat = "";
      final BfdRequestLog requestLog =
          new BfdRequestLog(new Slf4jRequestLogWriter(), requestLogFormat);

      server.setRequestLog(requestLog);

      /*
       * Configure authentication for webapps. Note that this is a distinct operation from configuring
       * mutual TLS above via the SslContextFactory, as that mostly only impacts the connections. In
       * order to expose the client cert auth info to the webapp, this additional config here is
       * needed.
       */
      ConstraintSecurityHandler securityHandler = new ConstraintSecurityHandler();
      securityHandler.setAuthenticator(new SslClientCertAuthenticator(sslContextFactory));
      securityHandler.setLoginService(new ClientCertLoginService());
      ConstraintMapping constraintMapping = new ConstraintMapping();
      constraintMapping.setPathSpec("/*");
      constraintMapping.setConstraint(Constraint.SECURE_TRANSPORT);
      securityHandler.setConstraintMappings(new ConstraintMapping[] {constraintMapping});
      webapp.setSecurityHandler(securityHandler);

      // Wire up the WebAppContext to Jetty.
      ContextHandlerCollection handlers = new ContextHandlerCollection(webapp);
      server.setHandler(handlers);
      return new ServerInfo(server, webapp);
    }
  }

  /**
   * This method is the one that will get called when users launch the application from the command
   * line.
   *
   * @param args (should be empty, as this application accepts configuration via environment
   *     variables)
   */
  public static void main(String[] args) {
    LOGGER.info("Launcher starting up!");

    // Configure Java Util Logging (JUL) to route over SLF4J, instead.
    SLF4JBridgeHandler.removeHandlersForRootLogger(); // (since SLF4J 1.6.5)
    SLF4JBridgeHandler.install();

    // Ensure that unhandled exceptions are... well, handled. Kinda'.
    configureUnexpectedExceptionHandlers();

    // Parse the app config.
    AppConfiguration appConfig = null;
    try {
      ConfigLoader config =
          LayeredConfiguration.createConfigLoader(Map.of(), ConfigLoaderSource.fromEnv());
      appConfig = AppConfiguration.loadConfig(config);
      LOGGER.info("Launcher configured: '{}'", appConfig);
    } catch (ConfigException | AppConfigurationException e) {
      System.err.println(e.getMessage());
      LOGGER.warn("Invalid app configuration.", e);
      System.exit(EXIT_CODE_BAD_CONFIG);
    }

    // Wire up metrics reporting.
    MetricRegistry appMetrics = new MetricRegistry();
    appMetrics.registerAll(new MemoryUsageGaugeSet());
    appMetrics.registerAll(new GarbageCollectorMetricSet());
    Slf4jReporter appMetricsReporter =
        Slf4jReporter.forRegistry(appMetrics).outputTo(LOGGER).build();
    appMetricsReporter.start(1, TimeUnit.HOURS);

    // Create the Jetty Server instance that will do most of our work.
    ServerInfo serverInfo = createServer(appConfig);

    // Configure shutdown handlers before starting everything up.
    serverInfo.server.setStopTimeout(Duration.ofSeconds(30).toMillis());
    serverInfo.server.setStopAtShutdown(true);
    registerShutdownHook(appMetrics);

    // Start up Jetty.
    try {
      LOGGER.info("Starting Jetty...");
      serverInfo.server.start();
      URI serverURI = serverInfo.server.getURI();
      LOGGER.info(
          "{} Server available at: '{}'",
          LOG_MESSAGE_STARTED_JETTY,
          String.format("https://%s:%d/", serverURI.getHost(), serverURI.getPort()));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    // This can be useful when debugging, as it prints Jetty's full config out.
    // server.dumpStdErr();
    // StatusPrinter.print((LoggerContext) LoggerFactory.getILoggerFactory());
    // StatusPrinter.print(requestLog.getStatusManager());

    /*
     * At this point, we're done here with the initial/launch thread. From now on, Jetty's threads
     * are the whole show. If someone sends a SIGINT signal to our process, the shutdown hooks
     * configured above will fire.
     */
    try {
      serverInfo.server.join();
    } catch (InterruptedException e) {
      // Don't do anything here; this is expected behavior when the app is stopped.
    }

    /*
     * Anything past this point is not guaranteed to run, as the thread may get stopped before it
     * has a chance to execute.
     */
  }

  /**
   * BFD implementation of the Jetty {@link org.eclipse.jetty.server.RequestLog} which provides
   * callback functionality appropriate for writing access logs which contain information for each
   * request received by the server.
   *
   * <p>This implementation is responsible for writing to the following log file upon completion of
   * each request:
   *
   * <ul>
   *   <li>access.json - a structured log built from the {@link BfdMDC}
   * </ul>
   */
  private static class BfdRequestLog extends CustomRequestLog {
    /**
     * Construct a BFD Request Log.
     *
     * @param writer source for the structured log
     * @param accessLogFormat format for the structured log
     */
    public BfdRequestLog(Slf4jRequestLogWriter writer, String accessLogFormat) {
      super(writer, accessLogFormat);
    }
  }

  /**
   * Registers a JVM shutdown hook that ensures that the application exits gracefully. Note that we
   * use Jetty's {@link Server#setStopAtShutdown(boolean)} method to gracefully stop Jetty (see code
   * above); this just handles anything else we want to do as part of a graceful shutdown.
   *
   * <p>The way the JVM handles all of this can be a bit surprising. Some observational notes:
   *
   * <ul>
   *   <li>If a user sends a <code>SIGINT</code> signal to the application (e.g. by pressing <code>
   *       ctrl+c</code>), the JVM will do the following: 1) it will run all registered shutdown
   *       hooks and wait for them to complete, and then 2) all threads will be stopped. No
   *       exceptions will be thrown on those threads that they could catch to prevent this; they
   *       just die.
   *   <li>If an application has a poorly designed shutdown hook that never completes, the
   *       application will never stop any of its threads or exit (in response to a <code>SIGINT
   *       </code>).
   *   <li>If all of an application's non-daemon threads complete, the application will then run all
   *       registered shutdown hooks and exit.
   *   <li>You can't call {@link System#exit(int)} (to set the exit code) inside a shutdown hook. If
   *       you do, the application will hang forever.
   *   <li>If a user sends a more aggressive <code>SIGKILL</code> signal to the application (e.g. by
   *       using their task manager), the JVM will just immediately stop all threads.
   *   <li>I haven't verified this in a while, but the <code>-Xrs</code> JVM option (which we're not
   *       using) should cause the application to completely ignore <code>SIGINT</code> signals.
   * </ul>
   *
   * @param metrics the {@link MetricRegistry} to log out before the application exits
   */
  private static void registerShutdownHook(MetricRegistry metrics) {
    Runtime.getRuntime()
        .addShutdownHook(
            new Thread(
                () -> {
                  LOGGER.info("Application is shutting down. Housekeeping...");

                  // Ensure that the final metrics get logged.
                  Slf4jReporter.forRegistry(metrics).outputTo(LOGGER).build().report();

                  LOGGER.info(LOG_MESSAGE_SHUTDOWN_HOOK_COMPLETE);

                  /*
                   * We have to do this ourselves (rather than use Logback's DelayingShutdownHook)
                   * to ensure that the logger isn't closed before the above logging.
                   */
                  LoggerContext logbackContext = (LoggerContext) LoggerFactory.getILoggerFactory();
                  logbackContext.stop();
                }));
  }

  /**
   * Registers {@link UncaughtExceptionHandler}s for the main thread, and a default one for all
   * other threads. These are just here to make sure that things don't die silently, but instead at
   * least log any errors that have occurred.
   */
  private static void configureUnexpectedExceptionHandlers() {
    Thread.currentThread()
        .setUncaughtExceptionHandler(
            (t, e) -> LOGGER.error("Uncaught exception on launch thread. Stopping.", e));
    Thread.setDefaultUncaughtExceptionHandler(
        (t, e) -> {
          /*
           * Just a note on something that I found a bit surprising: this won't be triggered for
           * errors that occur on a ScheduledExecutorService's threads, as the
           * ScheduledExecutorService swallows those exceptions.
           */

          LOGGER.error("Uncaught exception on non-main thread. Stopping.", e);
        });
  }
}
