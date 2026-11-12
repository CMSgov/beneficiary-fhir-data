package gov.cms.bfd.server.ng;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.gclient.IReadTyped;
import com.deblock.jsondiff.DiffGenerator;
import com.deblock.jsondiff.matcher.CompositeJsonMatcher;
import com.deblock.jsondiff.matcher.StrictJsonArrayPartialMatcher;
import com.deblock.jsondiff.matcher.StrictJsonObjectPartialMatcher;
import com.deblock.jsondiff.matcher.StrictPrimitivePartialMatcher;
import com.deblock.jsondiff.viewer.OnlyErrorDiffViewer;
import com.deblock.jsondiff.viewer.PatchDiffViewer;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import gov.cms.bfd.server.ng.testUtil.EobSnapshotCanonicalizer;
import gov.cms.bfd.server.ng.testUtil.SortingNodeFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import lombok.SneakyThrows;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Integration tests that compare the EOBs returned by the server against the output of the FML
 * toolchain for the same synthetic claims. These are opt-in since the FML output must be generated
 * beforehand with {@code just fml-claim --clm-uniq-id <id>}. Enable with {@code
 * -Dits.fml.parity=true}.
 */
@EnabledIfSystemProperty(named = "its.fml.parity", matches = "true")
class FmlParityIT extends IntegrationTestBase {
  private static final Path FML_OUTPUT_DIR =
      Path.of(System.getProperty("its.fml.output.dir", "../bfd-model-idr/out/claims"));
  private static final Path REPORT_DIR = Path.of("target", "fml-parity");
  private static final String FML_OUTPUT_FILE = "ExplanationOfBenefit.json";
  private static final List<String> IGNORED_META_FIELDS =
      List.of("lastUpdated", "versionId", "source");

  private IReadTyped<ExplanationOfBenefit> eobRead() {
    return getFhirClient().read().resource(ExplanationOfBenefit.class);
  }

  @ParameterizedTest
  @ValueSource(strings = {CLAIM_ID_ADJUDICATED})
  @SneakyThrows
  void eobMatchesFmlOutput(String claimId) {
    var fmlFile = FML_OUTPUT_DIR.resolve(claimId).resolve(FML_OUTPUT_FILE);
    assertTrue(
        Files.exists(fmlFile),
        String.format(
            "FML output file does not exist: %s%nRun just fml-claim --clm-uniq-id %s",
            fmlFile.toAbsolutePath(), claimId));

    // Both resources go through the same HAPI encoder so formatting differences are removed.
    var parser = FhirContext.forR4Cached().newJsonParser();
    var serverEob =
        eobRead().withId(claimId).withAdditionalHeader("IncludeTaxNumbers", "true").execute();
    var fmlEob = parser.parseResource(ExplanationOfBenefit.class, Files.readString(fmlFile));

    var serverJson = canonicalize(parser.encodeResourceToString(serverEob));
    var fmlJson = canonicalize(parser.encodeResourceToString(fmlEob));

    // Save both outputs so they can be viewed using a diff tool if desired.
    var reportDir = REPORT_DIR.resolve(claimId);
    Files.createDirectories(reportDir);
    Files.writeString(reportDir.resolve("server.json"), serverJson);
    Files.writeString(reportDir.resolve("fml.json"), fmlJson);

    var jsonDiff =
        DiffGenerator.diff(
            fmlJson,
            serverJson,
            new CompositeJsonMatcher(
                new StrictJsonArrayPartialMatcher(),
                new StrictJsonObjectPartialMatcher(),
                new StrictPrimitivePartialMatcher()));
    Files.writeString(
        reportDir.resolve("fml-vs-server.patch"), PatchDiffViewer.from(jsonDiff).toString());

    assertEquals(
        fmlJson,
        serverJson,
        String.format(
            "EOB (ID: %s) does not match the FML output.%n%s",
            claimId, OnlyErrorDiffViewer.from(jsonDiff)));
  }

  /**
   * Orders the EOB elements deterministically and removes meta fields that differ between runs.
   *
   * @param rawJson the encoded EOB
   * @return the canonical JSON string
   */
  @SneakyThrows
  private static String canonicalize(String rawJson) {
    var node = EobSnapshotCanonicalizer.canonicalize(rawJson);
    if (node.get("meta") instanceof ObjectNode meta) {
      meta.remove(IGNORED_META_FIELDS);
    }
    return JsonMapper.builder()
        .nodeFactory(new SortingNodeFactory())
        .build()
        .writerWithDefaultPrettyPrinter()
        .writeValueAsString(node);
  }
}
