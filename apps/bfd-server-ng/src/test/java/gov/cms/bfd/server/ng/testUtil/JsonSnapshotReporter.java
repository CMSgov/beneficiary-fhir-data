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

/**
 * Snapshot reporter implementation, handles pretty printing and comparing differently ordered
 * snapshots.
 */
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

    // Adds a helpful bit of text at the end that spits out the specific JSON paths that have diffs
    final var errorsView = OnlyErrorDiffViewer.from(jsondiff).toString();
    final var diff = PatchDiffViewer.from(jsondiff);
    var diffStr = diff.toString();

    // Generates a GitHub style diff with a few extra lines below and above the diff marker to add
    // context
    final var extraLines = 5;
    // Length of the header in the patch file
    final var headerLength = 2;
    final var newline = "\n";
    final var diffMarkerAdd = "+";
    final var diffMarkerSubtract = "-";
    final var diffLines = diffStr.split(newline);

    var linesInOldFile = 0;
    var linesInNewFile = 0;
    var result = new StringBuilder(newline);
    for (var i = 0; i < diffLines.length; i++) {
      if (i >= headerLength) {
        if (!diffLines[i].startsWith(diffMarkerAdd)) {
          linesInOldFile++;
        }
        if (!diffLines[i].startsWith(diffMarkerSubtract)) {
          linesInNewFile++;
        }
      }

      for (var j = Math.max(0, i - extraLines);
          j < Math.min(diffLines.length, i + extraLines);
          j++) {
        if (diffLines[j].startsWith(diffMarkerSubtract) || diffLines[j].startsWith(diffMarkerAdd)) {
          result.append(diffLines[i]).append(newline);
          break;
        }
      }
    }
    // Replace the default header of "@@ @@" because it doesn't render in Intellij without the line
    // numbers included.
    // The patch always starts at line 0 so we can hardcode this part in the header.
    diffStr =
        diffStr.replace(
            "@@ @@", String.format("@@ -0,%d +0,%d @@", linesInOldFile, linesInNewFile));
    // Save the diff to a patch file, so it can be viewed using a diff tool if desired
    FileUtils.writeStringToFile(
        SnapshotHelper.getPatchfile(getClass(), current.getName()),
        diffStr,
        StandardCharsets.UTF_8);
    throw new AssertionFailedError(result + newline + errorsView);
  }
}
