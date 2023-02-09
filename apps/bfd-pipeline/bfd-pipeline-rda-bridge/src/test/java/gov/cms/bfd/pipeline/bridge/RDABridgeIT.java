package gov.cms.bfd.pipeline.bridge;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.protobuf.MessageOrBuilder;
import gov.cms.bfd.pipeline.bridge.io.Sink;
import gov.cms.bfd.pipeline.bridge.model.BeneficiaryData;
import gov.cms.bfd.pipeline.bridge.util.DataSampler;
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
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import utils.TestUtils;

/** RDABridgeIT class is used for integration tests of the RDABridge. */
class RDABridgeIT {

  /** Sets the filename for the Bene History CSV. */
  private static final String BENE_HISTORY_CSV = "beneficiary_history.csv";
  /** Sets the filename for the expected fiss file. */
  private static final String EXPECTED_FISS = "expected-fiss.ndjson";
  /** Sets the filename for the expected mcs file. */
  private static final String EXPECTED_MCS = "expected-mcs.ndjson";
  /** Sets the filename for the expected attribution file. */
  private static final String EXPECTED_ATTRIBUTION = "expected-attribution.json";
  /** Sets the filename for the actual fiss file. */
  private static final String ACTUAL_FISS = "rda-fiss-test-5-18.ndjson";
  /** Sets the filename for the actual mcs file. */
  private static final String ACTUAL_MCS = "rda-mcs-test-1-4.ndjson";
  /** Sets the filename for the actual attribution file. */
  private static final String ACTUAL_ATTRIBUTION = "attribution.json";

  /**
   * Ensures that no exceptions are thrown while generating output.
   *
   * @throws IOException if there is a setup issue loading the test data
   */
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
          "-a",
          "true",
          "-x",
          "4",
          "-q",
          outputDir.resolve("attribution.json").toString(),
          "-t",
          resourcesDir.resolve("attribution-template.json").toString(),
          rifDir
        });

    Set<String> ignorePaths =
        Set.of("/timestamp", "/source/transmissionTimestamp", "/source/extractDate");

    List<String> expectedFissJson = Files.readAllLines(expectedDir.resolve(EXPECTED_FISS));
    List<String> actualFissJson = Files.readAllLines(outputDir.resolve(ACTUAL_FISS));
    TestUtils.assertJsonEquals(expectedFissJson, actualFissJson, ignorePaths);

    List<String> expectedMcsJson = Files.readAllLines(expectedDir.resolve(EXPECTED_MCS));
    List<String> actualMcsJson = Files.readAllLines(outputDir.resolve(ACTUAL_MCS));
    TestUtils.assertJsonEquals(expectedMcsJson, actualMcsJson, ignorePaths);

    String expectedAttribution =
        String.join("\n", Files.readAllLines(expectedDir.resolve(EXPECTED_ATTRIBUTION)));
    String actualAttribution =
        String.join("\n", Files.readAllLines(outputDir.resolve(ACTUAL_ATTRIBUTION)));
    assertEquals(
        expectedAttribution,
        actualAttribution,
        "Generated attribution file does not match expected.");
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

    try (Sink<MessageOrBuilder> testSink =
        new Sink<>() {
          @Override
          public void write(MessageOrBuilder value) {
            results.add(value);
          }

          @Override
          public void close() throws IOException {}
        }) {

      final int FISS_ID = 0;
      final int MCS_ID = 1;

      DataSampler<String> dataSampler =
          DataSampler.<String>builder()
              .maxValues(10_000)
              .registerSampleSet(FISS_ID, 0.5f)
              .registerSampleSet(MCS_ID, 0.5f)
              .build();

      assertDoesNotThrow(
          () -> {
            bridge.executeTransformation(
                RDABridge.SourceType.FISS,
                resourcesDir,
                inpatientData,
                new WrappedCounter(0),
                mbiMap,
                testSink,
                dataSampler,
                FISS_ID);
            bridge.executeTransformation(
                RDABridge.SourceType.MCS,
                resourcesDir,
                carrierData,
                new WrappedCounter(0),
                mbiMap,
                testSink,
                dataSampler,
                MCS_ID);

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
  }

  /**
   * Returns the path of the Bene_History_Csv file.
   *
   * @return returns the {@link Path} of the bene history file
   */
  private Path getResourcePath() {
    // ConstantConditions - It'll be there, don't worry.
    //noinspection ConstantConditions
    return new File(getClass().getClassLoader().getResource(BENE_HISTORY_CSV).getFile())
        .getParentFile()
        .toPath();
  }
}
