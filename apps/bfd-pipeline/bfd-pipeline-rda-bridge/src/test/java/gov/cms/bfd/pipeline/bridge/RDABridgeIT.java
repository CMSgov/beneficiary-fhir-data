package gov.cms.bfd.pipeline.bridge;

import static org.junit.Assert.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flipkart.zjsonpatch.JsonDiff;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.junit.Test;

public class RDABridgeIT {

  private static final String BENE_HISTORY_CSV = "beneficiary_history.csv";
  private static final String EXPECTED_FISS = "expected-fiss.ndjson";
  private static final String ACTUAL_FISS = "rda-fiss.ndjson";
  private static final String EXPECTED_MCS = "expected-mcs.ndjson";
  private static final String ACTUAL_MCS = "rda-mcs.ndjson";

  @Test
  public void shouldGenerateCorrectOutput() throws IOException {
    // ConstantConditions - It'll be there, don't worry.
    //noinspection ConstantConditions
    Path resourcesDir =
        new File(getClass().getClassLoader().getResource(BENE_HISTORY_CSV).getFile())
            .getParentFile()
            .toPath();
    String rifDir = resourcesDir.toString();
    Path outputDir = resourcesDir.resolve("output");
    Path expectedDir = resourcesDir.resolve("expected");

    RDABridge.main(
        new String[] {
          "-o",
          outputDir.toString(),
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
    List<String> actualFissJson = Files.readAllLines(outputDir.resolve(ACTUAL_FISS));
    assertJsonEquals(expectedFissJson, actualFissJson, ignorePaths);

    List<String> expectedMcsJson = Files.readAllLines(expectedDir.resolve(EXPECTED_MCS));
    List<String> actualMcsJson = Files.readAllLines(outputDir.resolve(ACTUAL_MCS));
    assertJsonEquals(expectedMcsJson, actualMcsJson, ignorePaths);
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
