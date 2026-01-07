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
    // Our custom JSON serializer should already normalize everything appropriately, so we can use
    // strict matching here.
    final var jsonMatcher =
        new CompositeJsonMatcher(
            new StrictJsonArrayPartialMatcher(),
            new StrictJsonObjectPartialMatcher(),
            new StrictPrimitivePartialMatcher());

    if (previous.getBody().isEmpty()) {
      return;
    }

    final var jsondiff = DiffGenerator.diff(current.getBody(), previous.getBody(), jsonMatcher);

    // Adds a helpful bit of text at the end that spits out the specific JSON paths that have diffs
    final var errorsView = OnlyErrorDiffViewer.from(jsondiff).toString();
    final var diff = PatchDiffViewer.from(jsondiff);

    final var diffStr = diff.toString();

    // Save the diff to a patch file, so it can be viewed using a diff tool if desired
    FileUtils.writeStringToFile(
        SnapshotHelper.getPatchfile(getClass(), current.getName()),
        diffStr,
        StandardCharsets.UTF_8);

    // Generates a GitHub style diff with a few extra lines below and above the diff marker to add
    // context
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
