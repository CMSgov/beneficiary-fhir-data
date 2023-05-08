package gov.cms.bfd.pipeline.rda.grpc.apps;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.MessageOrBuilder;
import com.google.protobuf.util.JsonFormat;
import gov.cms.bfd.pipeline.rda.grpc.RdaChange;
import gov.cms.bfd.pipeline.rda.grpc.source.GrpcResponseStream;
import gov.cms.bfd.pipeline.rda.grpc.source.RdaSourceConfig;
import gov.cms.bfd.sharedutils.config.ConfigLoader;
import gov.cms.mpsm.rda.v1.ClaimRequest;
import gov.cms.mpsm.rda.v1.RDAServiceGrpc;
import io.grpc.ClientCall;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.MethodDescriptor;
import io.grpc.stub.ClientCalls;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.time.Duration;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/** Opens the stream and prints out claims for Fiss and Mcs into a json file. */
public class StoreRdaJsonApp<T extends MessageOrBuilder> {
  /** Enum to determine whether to ushe FISS or MCS service. */
  private enum ClaimType {
    /** Get Fiss claims. */
    FISS(RDAServiceGrpc::getGetFissClaimsMethod),
    /** Get Mcs claims. */
    MCS(RDAServiceGrpc::getGetMcsClaimsMethod);

    /** Represents a supplier of the results. */
    private final Supplier<MethodDescriptor<ClaimRequest, ? extends MessageOrBuilder>> methodSource;

    /**
     * Constructor for ClaimType.
     *
     * @param methodSource sets the source to either Fiss or Mcs
     */
    ClaimType(Supplier<MethodDescriptor<ClaimRequest, ? extends MessageOrBuilder>> methodSource) {
      this.methodSource = methodSource;
    }
  }

  /**
   * Sets up the config parameters and converts the Fiss and Mcs claims results into json and puts
   * it into the output file.
   *
   * @param args to be passed in
   * @throws Exception if the file writer can't be opened or closed
   */
  public static void main(String[] args) throws Exception {
    if (args.length != 1) {
      System.err.println("usage: StoreRdaJsonApp config");
      System.exit(1);
    }
    final ConfigLoader loader =
        ConfigLoader.builder().addPropertiesFile(new File(args[0])).addSystemProperties().build();
    final Config config = new Config(loader);

    final ManagedChannel channel = createChannel(config);
    try {
      final GrpcResponseStream<? extends MessageOrBuilder> results = callService(config, channel);
      int received = 0;
      try (PrintWriter output = new PrintWriter(new FileWriter(config.outputFile))) {
        while (received < config.maxToReceive && results.hasNext()) {
          final MessageOrBuilder change = results.next();
          final String json = convertToJson(change);
          output.println(json);
          output.flush();
          received += 1;
          if (received < 100 || received % 100 == 0) {
            System.out.printf("%d: %s%n", received, json);
          }
        }
      }
      System.out.printf("received %d claims%n", received);
      System.out.println("cancelling stream...");
      results.cancelStream("finished reading");
    } finally {
      channel.shutdown();
      channel.awaitTermination(60, TimeUnit.SECONDS);
    }
  }

  /**
   * Creates the appropriate channel to be used.
   *
   * @param config to be pssed into to use
   * @return the correct grpc channel to use
   */
  private static ManagedChannel createChannel(Config config) {
    final ManagedChannelBuilder<?> channelBuilder =
        ManagedChannelBuilder.forAddress(config.grpcConfig.getHost(), config.grpcConfig.getPort());
    if (config.grpcConfig.getHost().equals("localhost")) {
      channelBuilder.usePlaintext();
    }
    return channelBuilder.build();
  }

  /**
   * Returns the GrpcResponse stream associated with the correct config values and its channel.
   *
   * @param config is for the config values
   * @param channel to use
   * @return the response stream for the correct claim type
   */
  private static GrpcResponseStream<? extends MessageOrBuilder> callService(
      Config config, ManagedChannel channel) {
    final ClaimRequest request =
        ClaimRequest.newBuilder().setSince(config.startingSequenceNumber).build();
    final MethodDescriptor<ClaimRequest, ? extends MessageOrBuilder> method =
        config.claimType.methodSource.get();
    final ClientCall<ClaimRequest, ? extends MessageOrBuilder> call =
        channel.newCall(method, config.grpcConfig.createCallOptions());
    Iterator<? extends MessageOrBuilder> iterator =
        ClientCalls.blockingServerStreamingCall(call, request);
    return new GrpcResponseStream<>(call, iterator);
  }

  /**
   * Converts the claim data to json.
   *
   * @param change the claims data coming in
   * @return a string that omits insignificant whitespace in the JSON output
   * @throws InvalidProtocolBufferException for the json formatter
   */
  private static String convertToJson(MessageOrBuilder change)
      throws InvalidProtocolBufferException {
    return JsonFormat.printer().omittingInsignificantWhitespace().print(change);
  }

  /** This class is for config values to be stored. */
  private static class Config {
    /** GRPC config to be stored and used. */
    final RdaSourceConfig grpcConfig;
    /** The claim type to be used. */
    private final ClaimType claimType;
    /** The maximum number of output to receive. */
    private final int maxToReceive;
    /** The output file to use. */
    private final File outputFile;
    /** The starting sequence number to process. */
    private final long startingSequenceNumber;

    /**
     * Constructor to set the correct config parameters.
     *
     * @param options for config values
     */
    private Config(ConfigLoader options) {
      claimType = options.enumValue("output.type", ClaimType.class);
      maxToReceive = options.intValue("output.maxCount", Integer.MAX_VALUE);
      outputFile = options.writeableFile("output.file");
      startingSequenceNumber = options.longOption("output.seq").orElse(RdaChange.MIN_SEQUENCE_NUM);
      grpcConfig =
          RdaSourceConfig.builder()
              .serverType(RdaSourceConfig.ServerType.Remote)
              .host(options.stringValue("api.host", "localhost"))
              .port(options.intValue("api.port", 5003))
              .authenticationToken(options.stringValue("api.token", ""))
              .maxIdle(Duration.ofSeconds(options.intValue("job.idleSeconds", Integer.MAX_VALUE)))
              .build();
    }
  }
}
