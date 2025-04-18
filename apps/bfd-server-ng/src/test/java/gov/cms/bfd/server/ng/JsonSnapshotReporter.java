package gov.cms.bfd.server.ng;

import au.com.origin.snapshots.Snapshot;
import au.com.origin.snapshots.reporters.SnapshotReporter;
import com.deblock.jsondiff.DiffGenerator;
import com.deblock.jsondiff.matcher.CompositeJsonMatcher;
import com.deblock.jsondiff.matcher.StrictJsonArrayPartialMatcher;
import com.deblock.jsondiff.matcher.StrictJsonObjectPartialMatcher;
import com.deblock.jsondiff.matcher.StrictPrimitivePartialMatcher;
import com.deblock.jsondiff.viewer.OnlyErrorDiffViewer;
import com.deblock.jsondiff.viewer.PatchDiffViewer;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Properties;
import org.apache.commons.io.FileUtils;
import org.opentest4j.AssertionFailedError;

public class JsonSnapshotReporter implements SnapshotReporter {
  @Override
  public boolean supportsFormat(String outputFormat) {
    return outputFormat.equals("json") || outputFormat.equals("fhir+json");
  }

  @Override
  public void report(Snapshot previous, Snapshot current) {
    final var jsonMatcher =
        new CompositeJsonMatcher(
            new StrictJsonArrayPartialMatcher(),
            new StrictJsonObjectPartialMatcher(),
            new StrictPrimitivePartialMatcher());
    if (previous.getBody().isEmpty()) {
      return;
    }
    final var jsondiff = DiffGenerator.diff(previous.getBody(), current.getBody(), jsonMatcher);

    final var errorsView = OnlyErrorDiffViewer.from(jsondiff).toString();
    final var diff = PatchDiffViewer.from(jsondiff);

    final var diffStr = diff.toString();
    try {
      Properties prop = new Properties();
      prop.load(this.getClass().getClassLoader().getResourceAsStream("snapshot.properties"));
      var snapshotDir = prop.getProperty("snapshot-dir");
      FileUtils.writeStringToFile(
          new File(
              Paths.get(
                      "src/test/java",
                      getClass().getPackageName().replace(".", "/"),
                      snapshotDir,
                      current.getName() + ".patch")
                  .toString()),
          diffStr,
          StandardCharsets.UTF_8);

    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    final var diffLines = diffStr.split("\n");
    StringBuilder result = new StringBuilder("\n");
    for (var i = 0; i < diffLines.length; i++) {
      for (var j = Math.max(0, i - 5); j < Math.min(diffLines.length, i + 5); j++) {
        if (diffLines[j].startsWith("-") || diffLines[j].startsWith("+")) {
          result.append(diffLines[i]).append("\n");
          break;
        }
      }
    }
    throw new AssertionFailedError(result + "\n" + errorsView);
  }
}
