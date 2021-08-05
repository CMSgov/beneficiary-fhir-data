package gov.cms.bfd.pipeline.rda.grpc.sink;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.MessageOrBuilder;
import com.google.protobuf.util.JsonFormat;
import gov.cms.mpsm.rda.v1.ClaimRequest;
import gov.cms.mpsm.rda.v1.RDAServiceGrpc;
import io.grpc.CallOptions;
import io.grpc.ClientCall;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.MethodDescriptor;
import io.grpc.stub.ClientCalls;
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
    final String host = option(args, 0, "localhost");
    final int port = Integer.parseInt(option(args, 1, "443"));
    final ClaimType claimType = ClaimType.valueOf(option(args, 2, "fiss").toUpperCase());
    final int maxToReceive = Integer.parseInt(option(args, 3, "100"));
    final String filename = option(args, 4, claimType.name() + ".ndjson");

    final ManagedChannel channel = createChannel(host, port);
    try {
      final Iterator<? extends MessageOrBuilder> results = callService(claimType, channel);
      int received = 0;
      try (PrintWriter output = new PrintWriter(new FileWriter(filename))) {
        while (received < maxToReceive && results.hasNext()) {
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

  private static Iterator<? extends MessageOrBuilder> callService(
      ClaimType claimType, ManagedChannel channel) {
    final ClaimRequest request = ClaimRequest.newBuilder().build();
    final MethodDescriptor<ClaimRequest, ? extends MessageOrBuilder> method =
        claimType.methodSource.get();
    final ClientCall<ClaimRequest, ? extends MessageOrBuilder> call =
        channel.newCall(method, CallOptions.DEFAULT);
    return ClientCalls.blockingServerStreamingCall(call, request);
  }

  private static String convertToJson(MessageOrBuilder change)
      throws InvalidProtocolBufferException {
    return JsonFormat.printer().omittingInsignificantWhitespace().print(change);
  }

  private static String option(String[] args, int index, String defaultValue) {
    return args.length > index ? args[index] : defaultValue;
  }
}
