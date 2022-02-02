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
 * <p>The design utilizes templates to build custom sql scripts based on user configuration.
 *
 * <h3>Example script</h3>
 *
 * <p>insert into my_table ("some_value") values {{#SQLValues
 * this}}('{{this}}'){{#if @last}};{{else}},{{/if}} {{/SQLValues}}
 *
 * <p>This would print
 *
 * <p>insert into my_table ("some_value") values ('value1'), ('value2'), ('value3'), --....
 * ('valueN');
 *
 * <p>A sublist can also be denoted
 *
 * <p>insert into my_table ("some_value") values {{#SQLValues this
 * 2}}('{{this}}'){{#if @last}};{{else}},{{/if}} {{/SQLValues}}
 *
 * <p>This would print
 *
 * <p>insert into my_table ("some_value") values ('value1'), ('value2');
 */
@Slf4j
@RequiredArgsConstructor
public class AttributionBuilder {

  private static final String LABEL_GROUP = "label";
  private static final String COUNT_GROUP = "count";

  private static final Pattern attributionMarker =
      Pattern.compile("%%(?<" + LABEL_GROUP + ">.+)-(?<" + COUNT_GROUP + ">\\d+)%%");

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
        Matcher matcher = attributionMarker.matcher(line.trim());

        if (matcher.matches()) {
          String label = matcher.group(LABEL_GROUP);
          long count = Long.parseLong(matcher.group(COUNT_GROUP));

          Iterator<String> attribution = dataSampler.iterator();

          long i = -1;

          while (attribution.hasNext() && ++i < count) {
            if (i > 0) {
              writer.write(",\n");
            }

            writer.write("(" + label + ", '" + attribution.next() + "')");
          }

          writer.write(";");
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
