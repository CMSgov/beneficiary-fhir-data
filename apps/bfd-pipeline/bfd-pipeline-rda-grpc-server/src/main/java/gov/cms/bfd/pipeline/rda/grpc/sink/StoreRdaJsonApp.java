package gov.cms.bfd.pipeline.rda.grpc.sink;

import com.google.protobuf.Empty;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import gov.cms.mpsm.rda.v1.ClaimChange;
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

public class StoreRdaJsonApp {
  private enum ClaimType {
    FISS(RDAServiceGrpc::getGetFissClaimsMethod),
    MCS(RDAServiceGrpc::getGetMcsClaimsMethod);

    private final Supplier<MethodDescriptor<Empty, ClaimChange>> methodSource;

    ClaimType(Supplier<MethodDescriptor<Empty, ClaimChange>> methodSource) {
      this.methodSource = methodSource;
    }
  }

  public static void main(String[] args) throws Exception {
    if (args.length != 1) {
      System.err.println("usage: DownloadRdaJsonApp config");
      System.exit(1);
    }
    final ConfigLoader loader =
        ConfigLoader.fromPropertiesFile(new File(args[0]))
            .withFallback(ConfigLoader.fromSystemProperties());
    final Config config = new Config(loader);

    final ManagedChannel channel = createChannel(config.apiHost, config.apiPort);
    try {
      final Iterator<ClaimChange> results = callService(config.claimType, channel);
      int received = 0;
      try (PrintWriter output = new PrintWriter(new FileWriter(config.outputFile))) {
        while (received < config.maxToReceive && results.hasNext()) {
          final ClaimChange change = results.next();
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
    } finally {
      channel.shutdown();
      channel.awaitTermination(5, TimeUnit.SECONDS);
    }
  }

  private static ManagedChannel createChannel(String host, int port) {
    final ManagedChannelBuilder<?> channelBuilder = ManagedChannelBuilder.forAddress(host, port);
    if (host.equals("localhost")) {
      channelBuilder.usePlaintext();
    }
    return channelBuilder.build();
  }

  private static Iterator<ClaimChange> callService(ClaimType claimType, ManagedChannel channel) {
    final Empty request = Empty.newBuilder().build();
    final MethodDescriptor<Empty, ClaimChange> method = claimType.methodSource.get();
    final ClientCall<Empty, ClaimChange> call = channel.newCall(method, CallOptions.DEFAULT);
    return ClientCalls.blockingServerStreamingCall(call, request);
  }

  private static String convertToJson(ClaimChange change) throws InvalidProtocolBufferException {
    return JsonFormat.printer().omittingInsignificantWhitespace().print(change);
  }

  private static class Config {
    private final String apiHost;
    private final int apiPort;
    private final ClaimType claimType;
    private final int maxToReceive;
    private final File outputFile;

    private Config(ConfigLoader options) {
      apiHost = options.stringValue("api.host", "localhost");
      apiPort = options.intValue("api.port", 443);
      claimType = options.enumValue("output.type", ClaimType::valueOf);
      maxToReceive = options.intValue("output.maxCount", Integer.MAX_VALUE);
      outputFile = options.writeableFile("output.file");
    }
  }
}
