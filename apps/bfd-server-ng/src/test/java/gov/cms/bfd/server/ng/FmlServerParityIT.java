package gov.cms.bfd.server.ng;

import static org.junit.jupiter.api.Assertions.assertEquals;

import ca.uhn.fhir.context.FhirContext;
import com.deblock.jsondiff.DiffGenerator;
import com.deblock.jsondiff.matcher.CompositeJsonMatcher;
import com.deblock.jsondiff.matcher.StrictJsonArrayPartialMatcher;
import com.deblock.jsondiff.matcher.StrictJsonObjectPartialMatcher;
import com.deblock.jsondiff.matcher.StrictPrimitivePartialMatcher;
import com.deblock.jsondiff.viewer.OnlyErrorDiffViewer;
import com.deblock.jsondiff.viewer.PatchDiffViewer;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.json.JsonMapper;
import gov.cms.bfd.server.ng.testUtil.EobSnapshotCanonicalizer;
import gov.cms.bfd.server.ng.testUtil.JsonSnapshotSerializer;
import gov.cms.bfd.server.ng.testUtil.SortingNodeFactory;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

// Compares server EOBs to FML output. Generate the FML output first with
// `just fml-transform --clm-uniq-id <id>`, then run with -Dits.fml.parity=true
// -Dits.fml.claim.id=<id>.
@EnabledIfSystemProperty(named = "its.fml.parity", matches = "true")
class FmlServerParityIT extends IntegrationTestBase {
  private static final Path FML_DIR =
      Path.of(System.getProperty("its.fml.output.dir", "../bfd-model-idr/out/claims"));
  private static final String CLAIM_ID = System.getProperty("its.fml.claim.id");
  private static final Path REPORT_DIR = Path.of("target", "fml-parity");
  private static final String FML_FILE = "ExplanationOfBenefit.json";
  private static final ObjectWriter WRITER =
      JsonMapper.builder()
          .nodeFactory(new SortingNodeFactory())
          .build()
          .writerWithDefaultPrettyPrinter();

  static List<String> fmlClaimIds() throws IOException {
    // fml-claim names each output dir after the claim ID
    try (var dirs = Files.list(FML_DIR)) {
      return dirs.filter(d -> Files.exists(d.resolve(FML_FILE)))
          .map(d -> d.getFileName().toString())
          .filter(id -> CLAIM_ID == null || CLAIM_ID.equals(id))
          .sorted()
          .toList();
    }
  }

  @ParameterizedTest
  @MethodSource("fmlClaimIds")
  void eobMatchesFmlOutput(String claimId) throws IOException {
    var serverEob =
        getFhirClient()
            .read()
            .resource(ExplanationOfBenefit.class)
            .withId(claimId)
            .withAdditionalHeader("IncludeTaxNumbers", "true")
            .execute();
    var fmlEob =
        FhirContext.forR4Cached()
            .newJsonParser()
            .parseResource(
                ExplanationOfBenefit.class,
                Files.readString(FML_DIR.resolve(claimId).resolve(FML_FILE)));

    var serverJson = canonicalize(serverEob);
    var fmlJson = canonicalize(fmlEob);

    // written out for viewing in a diff tool
    var reportDir = REPORT_DIR.resolve(claimId);
    Files.createDirectories(reportDir);
    Files.writeString(reportDir.resolve("server.json"), serverJson);
    Files.writeString(reportDir.resolve("fml.json"), fmlJson);

    var diff =
        DiffGenerator.diff(
            fmlJson,
            serverJson,
            new CompositeJsonMatcher(
                new StrictJsonArrayPartialMatcher(),
                new StrictJsonObjectPartialMatcher(),
                new StrictPrimitivePartialMatcher()));
    Files.writeString(
        reportDir.resolve("fml-vs-server.patch"), PatchDiffViewer.from(diff).toString());

    assertEquals(
        fmlJson,
        serverJson,
        "EOB " + claimId + " does not match FML output\n" + OnlyErrorDiffViewer.from(diff));
  }

  private static String canonicalize(ExplanationOfBenefit eob) throws IOException {
    return WRITER.writeValueAsString(
        EobSnapshotCanonicalizer.canonicalize(JsonSnapshotSerializer.toJson(eob)));
  }
}
