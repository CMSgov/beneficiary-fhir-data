package gov.cms.bfd.pipeline.bridge;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flipkart.zjsonpatch.JsonDiff;
import com.google.protobuf.MessageOrBuilder;
import gov.cms.bfd.pipeline.bridge.io.Sink;
import gov.cms.bfd.pipeline.bridge.model.BeneficiaryData;
import gov.cms.bfd.pipeline.bridge.util.WrappedCounter;
import gov.cms.bfd.pipeline.rda.grpc.sink.direct.MbiCache;
import gov.cms.bfd.pipeline.rda.grpc.source.FissClaimTransformer;
import gov.cms.bfd.pipeline.rda.grpc.source.McsClaimTransformer;
import gov.cms.bfd.pipeline.sharedutils.IdHasher;
import gov.cms.mpsm.rda.v1.FissClaimChange;
import gov.cms.mpsm.rda.v1.McsClaimChange;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class RDABridgeIT {

  private static final String BENE_HISTORY_CSV = "beneficiary_history.csv";
  private static final String EXPECTED_FISS = "expected-fiss.ndjson";
  private static final String EXPECTED_MCS = "expected-mcs.ndjson";

  @Test
  void shouldGenerateCorrectOutput() throws IOException {
    Path resourcesDir = getResourcePath();
    String rifDir = resourcesDir.toString();
    Path outputDir = resourcesDir.resolve("output-test");
    Path expectedDir = resourcesDir.resolve("expected");

    RDABridge.main(
        new String[] {
          "-o",
          outputDir.toString(),
          "-b",
          "beneficiary_history.csv",
          "-g",
          "rda-fiss-test.ndjson",
          "-n",
          "rda-mcs-test.ndjson",
          "-f",
          "inpatient.csv",
          "-f",
          "outpatient.csv",
          "-f",
          "home.csv",
          "-f",
          "hospice.csv",
          "-f",
          "snf.csv",
          "-m",
          "carrier.csv",
          "-s",
          "5",
          "-z",
          "1",
          rifDir
        });

    Set<String> ignorePaths = Collections.singleton("/timestamp");

    List<String> expectedFissJson = Files.readAllLines(expectedDir.resolve(EXPECTED_FISS));
    List<String> actualFissJson =
        Files.readAllLines(outputDir.resolve("rda-fiss-test-5-18.ndjson"));
    assertJsonEquals(expectedFissJson, actualFissJson, ignorePaths);

    List<String> expectedMcsJson = Files.readAllLines(expectedDir.resolve(EXPECTED_MCS));
    List<String> actualMcsJson = Files.readAllLines(outputDir.resolve("rda-mcs-test-1-4.ndjson"));
    assertJsonEquals(expectedMcsJson, actualMcsJson, ignorePaths);
  }

  /**
   * Ensures that no exceptions are thrown while transforming Fiss and MCS claims.
   *
   * @throws IOException if there is a setup issue loading the test data
   */
  @Test
  void shouldProduceValidClaimStructures() throws IOException {
    RDABridge bridge = new RDABridge();

    Path resourcesDir = getResourcePath();
    String inpatientData = "inpatient.csv";
    String carrierData = "carrier.csv";

    Map<String, BeneficiaryData> mbiMap =
        bridge.parseMbiNumbers(resourcesDir.resolve("beneficiary_history.csv"));

    List<MessageOrBuilder> results = new ArrayList<>();

    Sink<MessageOrBuilder> testSink =
        new Sink<>() {
          @Override
          public void write(MessageOrBuilder value) {
            results.add(value);
          }

          @Override
          public void close() throws IOException {}
        };

    assertDoesNotThrow(
        () -> {
          bridge.executeTransformation(
              RDABridge.SourceType.FISS,
              resourcesDir,
              inpatientData,
              new WrappedCounter(0),
              mbiMap,
              testSink);
          bridge.executeTransformation(
              RDABridge.SourceType.MCS,
              resourcesDir,
              carrierData,
              new WrappedCounter(0),
              mbiMap,
              testSink);

          Clock clock = Clock.fixed(Instant.ofEpochMilli(1622743357000L), ZoneOffset.UTC);
          IdHasher.Config hasherConfig = new IdHasher.Config(10, "justsomestring");
          FissClaimTransformer fissTransformer =
              new FissClaimTransformer(clock, MbiCache.computedCache(hasherConfig));
          McsClaimTransformer mcsTransformer =
              new McsClaimTransformer(clock, MbiCache.computedCache(hasherConfig));

          for (MessageOrBuilder message : results) {
            if (message instanceof FissClaimChange) {
              fissTransformer.transformClaim((FissClaimChange) message);
            } else {
              mcsTransformer.transformClaim((McsClaimChange) message);
            }
          }
        });
  }

  private void assertJsonEquals(
      List<String> expectedJson, List<String> actualJson, Set<String> ignorePaths) {
    int i;

    List<String> diffList = new ArrayList<>();

    for (i = 0; i < expectedJson.size() && i < actualJson.size(); ++i) {
      String diffMessage = createDiff(expectedJson.get(i), actualJson.get(i), ignorePaths);

      if (!diffMessage.isEmpty()) {
        diffList.add("Entry " + (i + 1) + ": " + diffMessage);
      }
    }

    if (i < actualJson.size()) {
      for (int j = i; j < actualJson.size(); ++j) {
        String diffMessage = createDiff("{}", actualJson.get(j), ignorePaths);

        if (!diffMessage.isEmpty()) {
          diffList.add("Entry " + (j + 1) + ": " + diffMessage);
        }
      }
    } else if (i < expectedJson.size()) {
      for (int j = i; j < expectedJson.size(); ++j) {
        String diffMessage = createDiff(expectedJson.get(j), "{}", ignorePaths);

        if (!diffMessage.isEmpty()) {
          diffList.add("Entry " + (j + 1) + ": " + diffMessage);
        }
      }
    }

    assertTrue(diffList.isEmpty(), String.join("\n", diffList));
  }

  private String createDiff(String expectedJson, String actualJson, Set<String> ignorePaths) {
    String message;

    try {
      List<String> diffs = diffJson(expectedJson, actualJson, ignorePaths);
      message = String.join(",", diffs);
    } catch (IOException e) {
      message = "Failed to be read";
    }

    return message;
  }

  private List<String> diffJson(String expectedJson, String actualJson, Set<String> ignorePaths)
      throws IOException {
    ObjectMapper mapper = new ObjectMapper();

    JsonNode expected = mapper.readTree(expectedJson);
    JsonNode actual = mapper.readTree(actualJson);

    JsonNode diff = JsonDiff.asJson(expected, actual);

    List<String> diffs = new ArrayList<>();

    for (int i = 0; i < diff.size(); ++i) {
      if (!ignorePaths.contains(diff.get(i).get("path").asText())) {
        diffs.add(diff.get(i).toString());
      }
    }

    return diffs;
  }

  private Path getResourcePath() {
    // ConstantConditions - It'll be there, don't worry.
    //noinspection ConstantConditions
    return new File(getClass().getClassLoader().getResource(BENE_HISTORY_CSV).getFile())
        .getParentFile()
        .toPath();
  }
}
