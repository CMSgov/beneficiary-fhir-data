package gov.cms.bfd.pipeline.rda.grpc.sink;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.MessageOrBuilder;
import com.google.protobuf.util.JsonFormat;
import gov.cms.bfd.pipeline.rda.grpc.RdaChange;
import gov.cms.bfd.pipeline.rda.grpc.source.GrpcResponseStream;
import gov.cms.bfd.sharedutils.config.ConfigLoader;
import gov.cms.mpsm.rda.v1.ClaimRequest;
import gov.cms.mpsm.rda.v1.RDAServiceGrpc;
import io.grpc.CallOptions;
import io.grpc.ClientCall;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.MethodDescriptor;
import io.grpc.stub.ClientCalls;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class StoreRdaJsonApp<T extends MessageOrBuilder> {
  private enum ClaimType {
    FISS(RDAServiceGrpc::getGetFissClaimsMethod),
    MCS(RDAServiceGrpc::getGetMcsClaimsMethod);

    private final Supplier<MethodDescriptor<ClaimRequest, ? extends MessageOrBuilder>> methodSource;

    ClaimType(Supplier<MethodDescriptor<ClaimRequest, ? extends MessageOrBuilder>> methodSource) {
      this.methodSource = methodSource;
    }
  }

  public static void main(String[] args) throws Exception {
    if (args.length != 1) {
      System.err.println("usage: DownloadRdaJsonApp config");
      System.exit(1);
    }
    final ConfigLoader loader =
        ConfigLoader.builder().addPropertiesFile(new File(args[0])).addSystemProperties().build();
    final Config config = new Config(loader);

    final ManagedChannel channel = createChannel(config.apiHost, config.apiPort);
    try {
      final GrpcResponseStream<? extends MessageOrBuilder> results =
          callService(config.claimType, config.startingSequenceNumber, channel);
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

  private static ManagedChannel createChannel(String host, int port) {
    final ManagedChannelBuilder<?> channelBuilder = ManagedChannelBuilder.forAddress(host, port);
    if (host.equals("localhost")) {
      channelBuilder.usePlaintext();
    }
    return channelBuilder.build();
  }

  private static GrpcResponseStream<? extends MessageOrBuilder> callService(
      ClaimType claimType, long startingSequenceNumber, ManagedChannel channel) {
    final ClaimRequest request = ClaimRequest.newBuilder().setSince(startingSequenceNumber).build();
    final MethodDescriptor<ClaimRequest, ? extends MessageOrBuilder> method =
        claimType.methodSource.get();
    final ClientCall<ClaimRequest, ? extends MessageOrBuilder> call =
        channel.newCall(method, CallOptions.DEFAULT);
    Iterator<? extends MessageOrBuilder> iterator =
        ClientCalls.blockingServerStreamingCall(call, request);
    return new GrpcResponseStream<>(call, iterator);
  }

  private static String convertToJson(MessageOrBuilder change)
      throws InvalidProtocolBufferException {
    return JsonFormat.printer().omittingInsignificantWhitespace().print(change);
  }

  private static class Config {
    private final String apiHost;
    private final int apiPort;
    private final ClaimType claimType;
    private final int maxToReceive;
    private final File outputFile;
    private final long startingSequenceNumber;

    private Config(ConfigLoader options) {
      apiHost = options.stringValue("api.host", "localhost");
      apiPort = options.intValue("api.port", 443);
      claimType = options.enumValue("output.type", ClaimType::valueOf);
      maxToReceive = options.intValue("output.maxCount", Integer.MAX_VALUE);
      outputFile = options.writeableFile("output.file");
      startingSequenceNumber = options.longOption("output.seq").orElse(RdaChange.MIN_SEQUENCE_NUM);
    }
  }
}
