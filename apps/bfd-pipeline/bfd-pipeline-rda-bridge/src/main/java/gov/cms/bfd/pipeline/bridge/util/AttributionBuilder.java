package gov.cms.bfd.pipeline.bridge.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Helper class for building the attribution sql script.
 *
 * <p>The design utilizes a simple template with the syntax '%%formatstring%%iterations%%.
 *
 * <h3>Example script</h3>
 *
 * <p>[%%"%s",%%3%%]
 *
 * <p>This would print
 *
 * <p>["value1","value2","value3",]
 */
@Slf4j
@RequiredArgsConstructor
public class AttributionBuilder {

  private static final String TEMPLATE_GROUP = "TemplateGroup";
  private static final String FORMAT_GROUP = "FormatString";
  private static final String COUNT_GROUP = "Iterations";

  private static final Pattern attributionMarker =
      Pattern.compile(
          "(?<"
              + TEMPLATE_GROUP
              + ">%%(?<"
              + FORMAT_GROUP
              + ">.+)%%(?<"
              + COUNT_GROUP
              + ">\\d+)%%)");

  private final String attributionTemplate;
  private final String attributionScript;

  /**
   * Runs the attribution builder logic, reading the given template file and producing a new script
   * in the given file location, interpolated with the given {@link DataSampler} dataset.
   *
   * @param dataSampler The {@link DataSampler} set to pull data from.
   */
  public void run(DataSampler<String> dataSampler) {
    try (BufferedReader reader = new BufferedReader(new FileReader(attributionTemplate));
        BufferedWriter writer = new BufferedWriter(new FileWriter(attributionScript))) {
      String line;

      while ((line = reader.readLine()) != null) {
        Matcher matcher = attributionMarker.matcher(line);

        if (matcher.find()) {
          String stringFormat = matcher.group(FORMAT_GROUP);
          long count = Long.parseLong(matcher.group(COUNT_GROUP));
          int startMatch = matcher.start(TEMPLATE_GROUP);
          int endMatch = matcher.end(TEMPLATE_GROUP);

          writer.write(line.substring(0, startMatch));

          Iterator<String> attribution = dataSampler.iterator();

          long i = -1;

          while (attribution.hasNext() && ++i < count) {
            writer.write(String.format(stringFormat, attribution.next()));
          }

          writer.write(line.substring(endMatch));
        } else {
          writer.write(line);
        }

        writer.newLine();
      }
    } catch (IOException e) {
      log.error("Unable to create attribution sql script", e);
    }
  }
}
