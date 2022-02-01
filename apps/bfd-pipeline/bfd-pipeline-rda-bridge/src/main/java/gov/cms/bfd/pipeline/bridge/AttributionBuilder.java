package gov.cms.bfd.pipeline.bridge;

import gov.cms.bfd.sharedutils.config.ConfigLoader;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AttributionBuilder {

  private static final String LABEL_GROUP = "label";
  private static final String COUNT_GROUP = "count";

  private static final Pattern attributionMarker =
      Pattern.compile("%%(?<" + LABEL_GROUP + ">.+)-(?<" + COUNT_GROUP + ">\\d+)%%");

  private static final long DEFAULT_LIMIT_SIZE = 10_000;
  private static final String DEFAULT_OUTPUT_FILE = "output/attribution.sql";
  private static final String DEFAULT_INPUT_FILE = "attribution-template.sql";

  private final long limit;
  private final String attributionScript;
  private final String attributionTemplate;

  public AttributionBuilder(ConfigLoader config) {
    limit = config.longOption(AppConfig.Fields.attributionSetSize).orElse(DEFAULT_LIMIT_SIZE);

    attributionScript =
        config.stringOption(AppConfig.Fields.attributionScriptFile).orElse(DEFAULT_OUTPUT_FILE);

    attributionTemplate =
        config.stringOption(AppConfig.Fields.attributionTemplateFile).orElse(DEFAULT_INPUT_FILE);
  }

  public void run(Set<String> fissMbis, Set<String> mcsMbis) {
    Set<String> attributionSet = buildAttributionSetWithLimit(fissMbis, mcsMbis, limit);

    try (BufferedReader reader = new BufferedReader(new FileReader(attributionTemplate));
        BufferedWriter writer = new BufferedWriter(new FileWriter(attributionScript))) {
      String line;

      while ((line = reader.readLine()) != null) {
        Matcher matcher = attributionMarker.matcher(line.trim());

        if (matcher.matches()) {
          String label = matcher.group(LABEL_GROUP);
          long count = Long.parseLong(matcher.group(COUNT_GROUP));

          Iterator<String> attribution = attributionSet.iterator();

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

  private Set<String> buildAttributionSetWithLimit(
      Set<String> fissMbis, Set<String> mcsMbis, long limit) {
    Set<String> attributionSet = new HashSet<>();

    Iterator<String> fissSet = fissMbis.iterator();
    Iterator<String> mcsSet = mcsMbis.iterator();

    while (fissSet.hasNext() && mcsSet.hasNext() && attributionSet.size() < limit) {
      attributionSet.add(fissSet.next());
      attributionSet.add(mcsSet.next());
    }

    while (fissSet.hasNext() && attributionSet.size() < limit) {
      attributionSet.add(fissSet.next());
    }

    while (mcsSet.hasNext() && attributionSet.size() < limit) {
      attributionSet.add(mcsSet.next());
    }

    return attributionSet;
  }
}
