package gov.cms.bfd.pipeline.rda.grpc.server;

import com.google.common.base.Strings;
import gov.cms.bfd.pipeline.rda.grpc.ThrowableAction;
import gov.cms.bfd.pipeline.rda.grpc.ThrowableConsumer;
import gov.cms.mpsm.rda.v1.FissClaimChange;
import gov.cms.mpsm.rda.v1.McsClaimChange;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import java.io.IOException;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.Singular;

public class RdaServer {
  /**
   * Creates a local RDA API server for testing.
   *
   * @param config {@link LocalConfig} for the server
   * @return a running RDA API Server object
   */
  public static Server startLocal(LocalConfig config) throws IOException {
    return ServerBuilder.forPort(config.getPort())
        .addService(config.createService())
        .intercept(new SimpleAuthorizationInterceptor(config.getAuthorizedTokens()))
        .build()
        .start();
  }

  /**
   * Creates an in-process (no network connections involved) RDA API server for testing.
   *
   * @param config {@link InProcessConfig} for the server
   * @return a running RDA API Server object
   */
  public static Server startInProcess(InProcessConfig config) throws IOException {
    return InProcessServerBuilder.forName(config.getServerName())
        .addService(config.createService())
        .intercept(new SimpleAuthorizationInterceptor(config.getAuthorizedTokens()))
        .build()
        .start();
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
    final Server server = startLocal(config);
    try {
      action.accept(server.getPort());
    } finally {
      server.shutdown();
      server.awaitTermination(3, TimeUnit.MINUTES);
    }
  }

  /**
   * Starts a server, runs an action with a ManagedChannel to the server as a parameter, and then
   * shuts down the server once the action has finished running. InProcess servers have less
   * overhead than Local servers but still exercise most of the GRPC plumbing.
   *
   * @param fissSourceFactory factory to create a FissClaimChange MessageSource
   * @param mcsSourceFactory factory to create a McsClaimChange MessageSource
   * @param action the action to execute
   * @throws Exception any exception is passed through to the caller
   */
  public static void runWithInProcessServer(
      InProcessConfig config, ThrowableConsumer<ManagedChannel> action) throws Exception {
    final Server server = startInProcess(config);
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
      server.shutdown();
      server.awaitTermination(3, TimeUnit.MINUTES);
    }
  }

  /**
   * Starts a server, runs a test with a no parameter, and then shuts down the server once the test
   * has finished running. InProcess servers have less overhead than Local servers but still
   * exercise most of the GRPC plumbing. This assumes the test will know how to connect to the
   * server on its own without any parameters.
   *
   * @param fissSourceFactory factory to create a FissClaimChange MessageSource
   * @param mcsSourceFactory factory to create a McsClaimChange MessageSource
   * @param test the test to execute
   * @throws Exception any exception is passed through to the caller
   */
  public static void runWithInProcessServerNoParam(InProcessConfig config, ThrowableAction test)
      throws Exception {
    final Server server = startInProcess(config);
    try {
      test.act();
    } finally {
      server.shutdown();
      server.awaitTermination(3, TimeUnit.MINUTES);
    }
  }

  @Getter
  private abstract static class BaseConfig {
    /** Version for the RdaService to report when getVersion is called. */
    private final RdaService.Version version;

    /** Factory used to create {@link MessageSource<FissClaimChange>} objects on demand. */
    private final MessageSource.Factory<FissClaimChange> fissSourceFactory;

    /** Factory used to create {@link MessageSource<McsClaimChange>} objects on demand. */
    private final MessageSource.Factory<McsClaimChange> mcsSourceFactory;

    /**
     * Set of authorized tokens to authenticate clients. If empty no authentication is performed.
     */
    private final Set<String> authorizedTokens;

    BaseConfig(
        RdaService.Version version,
        MessageSource.Factory<FissClaimChange> fissSourceFactory,
        MessageSource.Factory<McsClaimChange> mcsSourceFactory,
        Set<String> authorizedTokens) {
      this.version = version != null ? version : RdaService.Version.builder().build();
      this.fissSourceFactory =
          fissSourceFactory != null ? fissSourceFactory : EmptyMessageSource.factory();
      this.mcsSourceFactory =
          mcsSourceFactory != null ? mcsSourceFactory : EmptyMessageSource.factory();
      this.authorizedTokens = authorizedTokens;
    }

    /** @return properly configured RdaService instance */
    public RdaService createService() {
      return RdaService.Config.builder()
          .version(version)
          .fissSourceFactory(fissSourceFactory)
          .mcsSourceFactory(mcsSourceFactory)
          .build()
          .createService();
    }
  }

  /** Configuration data for running a server on a local port. */
  @Getter
  public static class LocalConfig extends BaseConfig {
    /**
     * The port for the server to listen on. A value of zero causes the server to allocate any open
     * port. Default for the builder is zero.
     */
    private final int port;

    @Builder
    private LocalConfig(
        RdaService.Version version,
        MessageSource.Factory<FissClaimChange> fissSourceFactory,
        MessageSource.Factory<McsClaimChange> mcsSourceFactory,
        @NonNull @Singular Set<String> authorizedTokens,
        int port) {
      super(version, fissSourceFactory, mcsSourceFactory, authorizedTokens);
      this.port = port;
    }

    /**
     * Shorthand for calling {@link RdaServer#runWithLocalServer(LocalConfig, ThrowableConsumer)}
     * with this config object.
     */
    public void runWithPortParam(ThrowableConsumer<Integer> action) throws Exception {
      runWithLocalServer(this, action);
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

    @Builder
    private InProcessConfig(
        RdaService.Version version,
        MessageSource.Factory<FissClaimChange> fissSourceFactory,
        MessageSource.Factory<McsClaimChange> mcsSourceFactory,
        @NonNull @Singular Set<String> authorizedTokens,
        String serverName) {
      super(version, fissSourceFactory, mcsSourceFactory, authorizedTokens);
      this.serverName = Strings.isNullOrEmpty(serverName) ? RdaServer.class.getName() : serverName;
    }

    /**
     * Shorthand for calling {@link RdaServer#runWithInProcessServer(InProcessConfig,
     * ThrowableConsumer)} with this config object.
     */
    public void runWithChannelParam(ThrowableConsumer<ManagedChannel> action) throws Exception {
      runWithInProcessServer(this, action);
    }

    /**
     * Shorthand for calling {@link RdaServer#runWithInProcessServerNoParam(InProcessConfig,
     * ThrowableAction)} with this config object.
     */
    public void runWithNoParam(ThrowableAction action) throws Exception {
      runWithInProcessServerNoParam(this, action);
    }
  }
}
