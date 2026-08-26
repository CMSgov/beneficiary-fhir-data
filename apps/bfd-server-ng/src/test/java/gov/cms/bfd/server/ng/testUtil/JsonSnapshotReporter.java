package gov.cms.bfd.server.ng.testUtil;

import au.com.origin.snapshots.Snapshot;
import au.com.origin.snapshots.reporters.SnapshotReporter;
import com.deblock.jsondiff.DiffGenerator;
import com.deblock.jsondiff.matcher.CompositeJsonMatcher;
import com.deblock.jsondiff.matcher.StrictJsonArrayPartialMatcher;
import com.deblock.jsondiff.matcher.StrictJsonObjectPartialMatcher;
import com.deblock.jsondiff.matcher.StrictPrimitivePartialMatcher;
import com.deblock.jsondiff.viewer.OnlyErrorDiffViewer;
import com.deblock.jsondiff.viewer.PatchDiffViewer;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.nio.charset.StandardCharsets;
import lombok.SneakyThrows;
import org.apache.commons.io.FileUtils;
import org.opentest4j.AssertionFailedError;

public class JsonSnapshotReporter implements SnapshotReporter {

  @Override
  public boolean supportsFormat(String outputFormat) {
    return outputFormat.equals("json") || outputFormat.equals("fhir+json");
  }

  @SneakyThrows
  @Override
  public void report(Snapshot previous, Snapshot current) {
    if (previous.getBody().isEmpty()) {
      return;
    }

    // We need to canonicalize the both the snapshot and the test output so that the diff is
    // meaningful and not randomly
    // mismatched or unordered issues.
    var keyOrderingObjectMapper =
        JsonMapper.builder().nodeFactory(new SortingNodeFactory()).build();

    final var previousCanonical =
        keyOrderingObjectMapper
            .writerWithDefaultPrettyPrinter()
            .writeValueAsString(EobSnapshotCanonicalizer.canonicalize(previous.getBody()));
    final var currentCanonical =
        keyOrderingObjectMapper
            .writerWithDefaultPrettyPrinter()
            .writeValueAsString(EobSnapshotCanonicalizer.canonicalize(current.getBody()));

    final var jsonMatcher =
        new CompositeJsonMatcher(
            new StrictJsonArrayPartialMatcher(),
            new StrictJsonObjectPartialMatcher(),
            new StrictPrimitivePartialMatcher());

    final var jsondiff = DiffGenerator.diff(previousCanonical, currentCanonical, jsonMatcher);

    final var errorsView = OnlyErrorDiffViewer.from(jsondiff).toString();
    final var diff = PatchDiffViewer.from(jsondiff);
    final var diffStr = diff.toString();

    FileUtils.writeStringToFile(
        SnapshotHelper.getPatchfile(getClass(), current.getName()),
        diffStr,
        StandardCharsets.UTF_8);

    final var extraLines = 5;
    final var newline = "\n";
    final var diffMarkerAdd = "+";
    final var diffMarkerSubtract = "-";
    final var diffLines = diffStr.split(newline);
    StringBuilder result = new StringBuilder(newline);
    for (var i = 0; i < diffLines.length; i++) {
      for (var j = Math.max(0, i - extraLines);
          j < Math.min(diffLines.length, i + extraLines);
          j++) {
        if (diffLines[j].startsWith(diffMarkerSubtract) || diffLines[j].startsWith(diffMarkerAdd)) {
          result.append(diffLines[i]).append(newline);
          break;
        }
      }
    }
    throw new AssertionFailedError(result + newline + errorsView);
  }
}
