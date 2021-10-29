package gov.cms.bfd.pipeline.bridge;

import static org.junit.Assert.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flipkart.zjsonpatch.JsonDiff;
import com.google.protobuf.MessageOrBuilder;
import gov.cms.bfd.pipeline.bridge.io.Sink;
import gov.cms.bfd.pipeline.bridge.model.BeneficiaryData;
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
import org.junit.Test;

public class RDABridgeIT {

  private static final String BENE_HISTORY_CSV = "beneficiary_history.csv";
  private static final String EXPECTED_FISS = "expected-fiss.ndjson";
  private static final String ACTUAL_FISS = "rda-fiss-test";
  private static final String EXPECTED_MCS = "expected-mcs.ndjson";
  private static final String ACTUAL_MCS = "rda-mcs-test";

  @Test
  public void shouldGenerateCorrectOutput() throws IOException {
    // ConstantConditions - It'll be there, don't worry.
    //noinspection ConstantConditions
    Path resourcesDir =
        new File(getClass().getClassLoader().getResource(BENE_HISTORY_CSV).getFile())
            .getParentFile()
            .toPath();
    String rifDir = resourcesDir.toString();
    Path outputDir = resourcesDir.resolve("output-test");
    Path expectedDir = resourcesDir.resolve("expected");

    RDABridge.main(
        new String[] {
          "-o",
          outputDir.toString(),
          "-g",
          ACTUAL_FISS,
          "-n",
          ACTUAL_MCS,
          "-f",
          "inpatient",
          "-f",
          "outpatient",
          "-f",
          "home",
          "-f",
          "hospice",
          "-f",
          "snf",
          "-m",
          "carrier",
          rifDir
        });

    Set<String> ignorePaths = Collections.emptySet();

    List<String> expectedFissJson = Files.readAllLines(expectedDir.resolve(EXPECTED_FISS));
    List<String> actualFissJson = Files.readAllLines(outputDir.resolve(ACTUAL_FISS + ".ndjson"));
    assertJsonEquals(expectedFissJson, actualFissJson, ignorePaths);

    List<String> expectedMcsJson = Files.readAllLines(expectedDir.resolve(EXPECTED_MCS));
    List<String> actualMcsJson = Files.readAllLines(outputDir.resolve(ACTUAL_MCS + ".ndjson"));
    assertJsonEquals(expectedMcsJson, actualMcsJson, ignorePaths);
  }

  // DefaultAnnotationParam - Might as well be explicit
  @SuppressWarnings("DefaultAnnotationParam")
  @Test(expected = Test.None.class)
  public void shouldProduceValidClaimStructures() throws IOException {
    RDABridge bridge = new RDABridge();

    // ConstantConditions - It'll be there, don't worry.
    //noinspection ConstantConditions
    Path resourcesDir =
        new File(getClass().getClassLoader().getResource(BENE_HISTORY_CSV).getFile())
            .getParentFile()
            .toPath();
    String inpatientData = resourcesDir.resolve("inpatient").toString();
    String carrierData = resourcesDir.resolve("carrier").toString();

    Map<String, BeneficiaryData> mbiMap = bridge.parseMbiNumbers(resourcesDir);

    List<MessageOrBuilder> results = new ArrayList<>();

    Sink<MessageOrBuilder> testSink =
        new Sink<MessageOrBuilder>() {
          @Override
          public void write(MessageOrBuilder value) {
            results.add(value);
          }

          @Override
          public void close() throws IOException {}
        };

    bridge.executeTransformation(
        RDABridge.SourceType.FISS, resourcesDir, inpatientData, mbiMap, testSink);
    bridge.executeTransformation(
        RDABridge.SourceType.MCS, resourcesDir, carrierData, mbiMap, testSink);

    Clock clock = Clock.fixed(Instant.ofEpochMilli(1622743357000L), ZoneOffset.UTC);
    IdHasher hasher = new IdHasher(new IdHasher.Config(10, "justsomestring"));
    FissClaimTransformer fissTransformer = new FissClaimTransformer(clock, hasher);
    McsClaimTransformer mcsTransformer = new McsClaimTransformer(clock, hasher);

    for (MessageOrBuilder message : results) {
      if (message instanceof FissClaimChange) {
        fissTransformer.transformClaim((FissClaimChange) message);
      } else {
        mcsTransformer.transformClaim((McsClaimChange) message);
      }
    }
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

    assertTrue(String.join("\n", diffList), diffList.isEmpty());
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
}
