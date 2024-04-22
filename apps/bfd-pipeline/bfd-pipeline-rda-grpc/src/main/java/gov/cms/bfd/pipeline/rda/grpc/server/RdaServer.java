package gov.cms.bfd.pipeline.rda.grpc.server;

import com.google.common.base.Strings;
import gov.cms.bfd.pipeline.rda.grpc.ThrowableAction;
import gov.cms.bfd.pipeline.rda.grpc.ThrowableConsumer;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import jakarta.annotation.Nullable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.Singular;

/** Class for creating a local RDA server for testing purposes. */
public class RdaServer {
  /**
   * Creates a local RDA API server for testing. Caller must close the result object to clean up
   * resources once the server has been terminated.
   *
   * @param config {@link LocalConfig} for the server
   * @return a running RDA API Server object and closeable state
   * @throws IOException if the server cannot bind at runtime
   */
  public static ServerState startLocal(LocalConfig config) throws Exception {
    final ServerBuilder<?> serverBuilder =
        config.hasHostname()
            ? NettyServerBuilder.forAddress(
                new InetSocketAddress(config.getHostname(), config.getPort()))
            : ServerBuilder.forPort(config.getPort());
    final var messageSourceFactory = config.getServiceConfig().createMessageSourceFactory();
    try {
      final var server =
          serverBuilder
              .addService(config.createService(messageSourceFactory))
              .intercept(new SimpleAuthorizationInterceptor(config.getAuthorizedTokens()))
              .build()
              .start();
      return new ServerState(server, messageSourceFactory);
    } catch (Exception ex) {
      // clean up if an exception was thrown since the caller would not be able to
      messageSourceFactory.close();
      throw ex;
    }
  }

  /**
   * Creates an in-process (no network connections involved) RDA API server for testing. Caller must
   * close the result object to clean up resources once the server has been terminated.
   *
   * @param config {@link InProcessConfig} for the server
   * @return a running RDA API Server object and closeable state
   * @throws IOException if the server cannot bind properly
   */
  public static ServerState startInProcess(InProcessConfig config) throws Exception {
    final var messageSourceFactory = config.getServiceConfig().createMessageSourceFactory();
    try {
      final var server =
          InProcessServerBuilder.forName(config.getServerName())
              .addService(config.createService(messageSourceFactory))
              .intercept(new SimpleAuthorizationInterceptor(config.getAuthorizedTokens()))
              .build()
              .start();
      return new ServerState(server, messageSourceFactory);
    } catch (Exception ex) {
      // clean up if an exception was thrown since the caller would not be able to
      messageSourceFactory.close();
      throw ex;
    }
  }

  /**
   * Starts a server, runs an action with the server's port as a parameter, and then shuts down the
   * server once the action has finished running.
   *
   * @param config {@link LocalConfig} for the server
   * @param action the action to execute
   * @throws Exception any exception is passed through to the caller
   */
  public static void runWithLocalServer(LocalConfig config, ThrowableConsumer<Integer> action)
      throws Exception {
    try (ServerState state = startLocal(config)) {
      try {
        action.accept(state.server.getPort());
      } finally {
        state.server.shutdown();
        state.server.awaitTermination(3, TimeUnit.MINUTES);
      }
    }
  }

  /**
   * Starts a server, runs an action with a ManagedChannel to the server as a parameter, and then
   * shuts down the server once the action has finished running. InProcess servers have less
   * overhead than Local servers but still exercise most of the GRPC plumbing.
   *
   * @param config the config for the server
   * @param action the action to execute
   * @throws Exception any exception is passed through to the caller
   */
  public static void runWithInProcessServer(
      InProcessConfig config, ThrowableConsumer<ManagedChannel> action) throws Exception {
    try (ServerState state = startInProcess(config)) {
      try {
        final ManagedChannel channel =
            InProcessChannelBuilder.forName(config.getServerName()).build();
        try {
          action.accept(channel);
        } finally {
          channel.shutdown();
          channel.awaitTermination(3, TimeUnit.MINUTES);
        }
      } finally {
        state.server.shutdown();
        state.server.awaitTermination(3, TimeUnit.MINUTES);
      }
    }
  }

  /**
   * Starts a server, runs a test with a no parameter, and then shuts down the server once the test
   * has finished running. InProcess servers have less overhead than Local servers but still
   * exercise most of the GRPC plumbing. This assumes the test will know how to connect to the
   * server on its own without any parameters.
   *
   * @param config the config for the server
   * @param test the test to execute
   * @throws Exception any exception is passed through to the caller
   */
  public static void runWithInProcessServerNoParam(InProcessConfig config, ThrowableAction test)
      throws Exception {
    try (ServerState state = startInProcess(config)) {
      try {
        test.act();
      } finally {
        state.server.shutdown();
        state.server.awaitTermination(3, TimeUnit.MINUTES);
      }
    }
  }

  /** Configuration for the RDA server. */
  @Getter
  private abstract static class BaseConfig {
    /** Configuration used to create an {@link RdaService}. */
    private final RdaMessageSourceFactory.Config serviceConfig;

    /**
     * Set of authorized tokens to authenticate clients. If empty no authentication is performed.
     */
    private final Set<String> authorizedTokens;

    /**
     * Instantiates a new configuration. If {@code serviceConfig} is not null it is used otherwise
     * the other parameters are used to build a config.
     *
     * @param serviceConfig config used to create {@link RdaService}
     * @param authorizedTokens the authorized tokens
     */
    BaseConfig(RdaMessageSourceFactory.Config serviceConfig, Set<String> authorizedTokens) {
      this.serviceConfig = serviceConfig;
      this.authorizedTokens = authorizedTokens;
    }

    /**
     * Creates a configured rda service with the provided {@link RdaMessageSourceFactory}.
     *
     * @param messageSourceFactory source of claims
     * @return properly configured RdaService instance
     */
    public RdaService createService(RdaMessageSourceFactory messageSourceFactory) {
      return new RdaService(messageSourceFactory);
    }
  }

  /** Configuration data for running a server on a local port. */
  @Getter
  public static class LocalConfig extends BaseConfig {
    /** The server hostname. */
    private final String hostname;

    /**
     * The port for the server to listen on. A value of zero causes the server to allocate any open
     * port. Default for the builder is zero.
     */
    private final int port;

    /**
     * Creates a new configuration. If {@code serviceConfig} is not null it is used otherwise the
     * other parameters are used to build a config.
     *
     * @param serviceConfig config used to create {@link RdaService}
     * @param authorizedTokens the authorized tokens
     * @param hostname the hostname
     * @param port the port
     */
    @Builder
    private LocalConfig(
        RdaMessageSourceFactory.Config serviceConfig,
        @NonNull @Singular Set<String> authorizedTokens,
        String hostname,
        int port) {
      super(serviceConfig, authorizedTokens);
      this.hostname = Strings.isNullOrEmpty(hostname) ? "localhost" : hostname;
      this.port = port;
    }

    /**
     * Shorthand for calling {@link RdaServer#runWithLocalServer(LocalConfig, ThrowableConsumer)}
     * with this config object.
     *
     * @param action the action
     * @throws Exception any exception running the action
     */
    public void runWithPortParam(ThrowableConsumer<Integer> action) throws Exception {
      runWithLocalServer(this, action);
    }

    /**
     * Determines if this server has a hostname.
     *
     * @return {@code true} if this server has a hostname
     */
    public boolean hasHostname() {
      return !Strings.isNullOrEmpty(hostname);
    }
  }

  /** Configuration data for running an in-process server. */
  @Getter
  public static class InProcessConfig extends BaseConfig {
    /**
     * Server name to use when starting the in-process server. Defaults to {@code
     * RdaServer.class.getName()}.
     */
    private final String serverName;

    /**
     * Instantiates a new configuration. If {@code serviceConfig} is not null it is used otherwise
     * the other parameters are used to build a config.
     *
     * @param serviceConfig config used to create {@link RdaService}
     * @param authorizedTokens the authorized tokens
     * @param serverName optional server name
     */
    @Builder
    private InProcessConfig(
        RdaMessageSourceFactory.Config serviceConfig,
        @NonNull @Singular Set<String> authorizedTokens,
        @Nullable String serverName) {
      super(serviceConfig, authorizedTokens);
      this.serverName = Strings.isNullOrEmpty(serverName) ? RdaServer.class.getName() : serverName;
    }

    /**
     * Shorthand for calling {@link RdaServer#runWithInProcessServer(InProcessConfig,
     * ThrowableConsumer)} with this config object.
     *
     * @param action the action to execute
     * @throws Exception any exception is passed through to the caller
     */
    public void runWithChannelParam(ThrowableConsumer<ManagedChannel> action) throws Exception {
      runWithInProcessServer(this, action);
    }

    /**
     * Shorthand for calling {@link RdaServer#runWithInProcessServerNoParam(InProcessConfig,
     * ThrowableAction)}* with this config object.
     *
     * @param action the action to execute
     * @throws Exception any exception is passed through to the caller
     */
    public void runWithNoParam(ThrowableAction action) throws Exception {
      runWithInProcessServerNoParam(this, action);
    }
  }

  /**
   * An {@link AutoCloseable} containing the {@link Server} that has been started. Closing this
   * after the server has terminated will clean up any resources used by the {@link RdaService}.
   */
  @AllArgsConstructor
  public static class ServerState implements AutoCloseable {
    /** The server that has been started. */
    @Getter private final Server server;

    /** The state that needs to be closed once server has terminated. */
    private final AutoCloseable state;

    @Override
    public void close() throws Exception {
      state.close();
    }
  }
}
