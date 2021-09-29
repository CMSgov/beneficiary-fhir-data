package gov.cms.bfd.server.launcher;

import ch.qos.logback.classic.LoggerContext;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Slf4jReporter;
import com.codahale.metrics.jvm.GarbageCollectorMetricSet;
import com.codahale.metrics.jvm.MemoryUsageGaugeSet;
import java.lang.Thread.UncaughtExceptionHandler;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.eclipse.jetty.annotations.AnnotationConfiguration;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.security.ConstraintMapping;
import org.eclipse.jetty.security.ConstraintSecurityHandler;
import org.eclipse.jetty.security.authentication.ClientCertAuthenticator;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.CustomRequestLog;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.server.handler.RequestLogHandler;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.security.Constraint;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.webapp.Configuration;
import org.eclipse.jetty.webapp.FragmentConfiguration;
import org.eclipse.jetty.webapp.JettyWebXmlConfiguration;
import org.eclipse.jetty.webapp.MetaInfConfiguration;
import org.eclipse.jetty.webapp.WebAppConfiguration;
import org.eclipse.jetty.webapp.WebAppContext;
import org.eclipse.jetty.webapp.WebInfConfiguration;
import org.eclipse.jetty.webapp.WebXmlConfiguration;
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
   * This {@link System#exit(int)} value should be used when the provided configuration values are
   * incomplete and/or invalid.
   */
  static final int EXIT_CODE_BAD_CONFIG = 1;

  /**
   * This {@link System#exit(int)} value should be used when the application exits due to an
   * unhandled exception.
   */
  static final int EXIT_CODE_MONITOR_ERROR = 2;

  private static Server server;

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
      appConfig = AppConfiguration.readConfigFromEnvironmentVariables();
      LOGGER.info("Launcher configured: '{}'", appConfig);
    } catch (AppConfigurationException e) {
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
    server = new Server(appConfig.getPort());

    // Modify the default HTTP config.
    HttpConfiguration httpConfig = new HttpConfiguration();
    httpConfig.setSecurePort(appConfig.getPort());

    // Create the HTTPS config.
    HttpConfiguration httpsConfig = new HttpConfiguration(httpConfig);
    httpsConfig.addCustomizer(new SecureRequestCustomizer());

    // Create the SslContextFactory to be used, along with the cert.
    SslContextFactory.Server sslContextFactory = new SslContextFactory.Server();
    sslContextFactory.setNeedClientAuth(true);
    sslContextFactory.setKeyStoreResource(Resource.newResource(appConfig.getKeystore()));
    sslContextFactory.setCertAlias("server");
    sslContextFactory.setKeyManagerPassword("changeit");
    sslContextFactory.setTrustStoreResource(Resource.newResource(appConfig.getTruststore()));
    sslContextFactory.setTrustStorePassword("changeit");
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
    ServerConnector serverConnector =
        new ServerConnector(
            server,
            new SslConnectionFactory(sslContextFactory, HttpVersion.HTTP_1_1.toString()),
            new HttpConnectionFactory(httpsConfig));
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
    webapp.getSystemClassMatcher().add("org.slf4j.");

    /*
     * Disable Logback's builtin shutdown hook, so that OUR shutdown hook can still use the loggers
     * (and then shut Logback down itself).
     */
    webapp.setInitParameter("logbackDisableServletContainerInitializer", "true");

    /*
     * Logback access does not seem to be supported any longer with jetty 10 so switching to
     * a custom format request log. Need to evaluate if we still need this at all and fill in
     * the format string to match what we had before or find a new approach entirely or
     * somehow hack the logback-access to make it work again.
     */
    RequestLogHandler requestLogHandler = new RequestLogHandler();
    final String accessLogFileName =
        System.getProperty("bfdServer.logs.dir", "./target/server-work/") + "access.log";
    String requestLogFormat =
        "%{remote}a - \"%u\" [%t] \"%r\" \"%q\" %s %{CLF}S %D %{BlueButton-OriginalQueryId}i %{BlueButton-OriginalQueryCounter}i [%{BlueButton-OriginalQueryTimestamp}i] %{BlueButton-DeveloperId}i \"%{BlueButton-Developer}i\" %{BlueButton-ApplicationId}i \"%{BlueButton-Application}i\" %{BlueButton-UserId}i \"%{BlueButton-User}i\" %{BlueButton-BeneficiaryId}i";
    CustomRequestLog requestLog = new CustomRequestLog(accessLogFileName, requestLogFormat);

    server.setRequestLog(requestLog);

    /*
     * Configure authentication for webapps. Note that this is a distinct operation from configuring
     * mutual TLS above via the SslContextFactory, as that mostly only impacts the connections. In
     * order to expose the client cert auth info to the webapp, this additional config here is
     * needed.
     */
    ConstraintSecurityHandler securityHandler = new ConstraintSecurityHandler();
    securityHandler.setAuthenticator(new ClientCertAuthenticator());
    securityHandler.setLoginService(new ClientCertLoginService());
    Constraint constraint = new Constraint();
    constraint.setName("auth");
    constraint.setAuthenticate(true);
    constraint.setRoles(new String[] {Constraint.ANY_AUTH});
    ConstraintMapping constraintMapping = new ConstraintMapping();
    constraintMapping.setPathSpec("/*");
    constraintMapping.setConstraint(constraint);
    securityHandler.setConstraintMappings(new ConstraintMapping[] {constraintMapping});
    webapp.setSecurityHandler(securityHandler);

    // Wire up the WebAppContext to Jetty.
    HandlerCollection handlers = new HandlerCollection(webapp, requestLogHandler);
    server.setHandler(handlers);

    // Configure shutdown handlers before starting everything up.
    server.setStopTimeout(Duration.ofSeconds(30).toMillis());
    server.setStopAtShutdown(true);
    registerShutdownHook(appMetrics);

    // Start up Jetty.
    try {
      LOGGER.info("Starting Jetty...");
      server.start();
      LOGGER.info(
          "{} Server available at: '{}'",
          LOG_MESSAGE_STARTED_JETTY,
          String.format("https://%s:%d/", server.getURI().getHost(), server.getURI().getPort()));
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
      server.join();
    } catch (InterruptedException e) {
      // Don't do anything here; this is expected behavior when the app is stopped.
    }

    /*
     * Anything past this point is not guaranteed to run, as the thread may get stopped before it
     * has a chance to execute.
     */
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
                new Runnable() {
                  @Override
                  public void run() {
                    LOGGER.info("Application is shutting down. Housekeeping...");

                    // Ensure that the final metrics get logged.
                    Slf4jReporter.forRegistry(metrics).outputTo(LOGGER).build().report();

                    LOGGER.info(LOG_MESSAGE_SHUTDOWN_HOOK_COMPLETE);

                    /*
                     * We have to do this ourselves (rather than use Logback's DelayingShutdownHook)
                     * to ensure that the logger isn't closed before the above logging.
                     */
                    LoggerContext logbackContext =
                        (LoggerContext) LoggerFactory.getILoggerFactory();
                    logbackContext.stop();
                  }
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
            new UncaughtExceptionHandler() {
              @Override
              public void uncaughtException(Thread t, Throwable e) {
                LOGGER.error("Uncaught exception on launch thread. Stopping.", e);
              }
            });
    Thread.setDefaultUncaughtExceptionHandler(
        new UncaughtExceptionHandler() {
          @Override
          public void uncaughtException(Thread t, Throwable e) {
            /*
             * Just a note on something that I found a bit surprising: this won't be triggered for
             * errors that occur on a ScheduledExecutorService's threads, as the
             * ScheduledExecutorService swallows those exceptions.
             */

            LOGGER.error("Uncaught exception on non-main thread. Stopping.", e);
          }
        });
  }
}
