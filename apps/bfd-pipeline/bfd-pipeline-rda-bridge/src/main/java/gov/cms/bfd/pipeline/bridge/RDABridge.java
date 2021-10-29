package gov.cms.bfd.pipeline.bridge;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.google.protobuf.MessageOrBuilder;
import gov.cms.bfd.pipeline.bridge.etl.AbstractTransformer;
import gov.cms.bfd.pipeline.bridge.etl.FissTransformer;
import gov.cms.bfd.pipeline.bridge.etl.McsTransformer;
import gov.cms.bfd.pipeline.bridge.etl.Parser;
import gov.cms.bfd.pipeline.bridge.etl.RifParser;
import gov.cms.bfd.pipeline.bridge.io.NdJsonSink;
import gov.cms.bfd.pipeline.bridge.io.RifSource;
import gov.cms.bfd.pipeline.bridge.io.Sink;
import gov.cms.bfd.pipeline.bridge.model.BeneficiaryData;
import gov.cms.bfd.sharedutils.config.ConfigLoader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.yaml.snakeyaml.Yaml;

@Slf4j
public class RDABridge {

  enum SourceType {
    FISS,
    MCS
  }

  private static final String OUTPUT_FLAG = "o";
  private static final String FISS_FLAG = "f";
  private static final String MCS_FLAG = "m";
  private static final String FISS_OUTPUT_FLAG = "g";
  private static final String MCS_OUTPUT_FLAG = "n";
  private static final String EXTERNAL_CONFIG_FLAG = "e";

  /**
   * Handles translation of a CLI execution, validating and pulling arguments to then invoke the
   * underlying application code with.
   *
   * @param args Array of the command line arguments.
   */
  public static void main(String[] args) {
    try {
      Options options =
          new Options()
              .addOption(
                  OUTPUT_FLAG, true, "The directory where the output files will be written to.")
              .addOption(FISS_FLAG, true, "FISS file to read from")
              .addOption(MCS_FLAG, true, "MCS file to read from")
              .addOption(FISS_OUTPUT_FLAG, true, "FISS RDA output file")
              .addOption(MCS_OUTPUT_FLAG, true, "MCS RDA output file")
              .addOption(EXTERNAL_CONFIG_FLAG, true, "Path to yaml file containing run configs");

      CommandLineParser parser = new DefaultParser();
      CommandLine cmd = parser.parse(options, args);

      ConfigLoader config;

      if (cmd.hasOption(EXTERNAL_CONFIG_FLAG)) {
        config = createYamlConfig(cmd.getOptionValue(EXTERNAL_CONFIG_FLAG));
      } else if (!cmd.getArgList().isEmpty()) {
        config = createCliConfig(cmd);
      } else {
        printUsage(options);
        System.exit(1);
        throw new IllegalStateException("This will never happen");
      }

      new RDABridge().run(config);
    } catch (IOException | ParseException e) {
      log.error("Failed to execute", e);
      System.exit(1);
    }
  }

  /**
   * Reads all relevant source files, executing task logic for each claim found.
   *
   * @param config The configurations to use when generating the RDA data.
   * @throws IOException If there was an issue accessing any of the files.
   */
  public void run(ConfigLoader config) throws IOException {
    Path path = Paths.get(config.stringValue(AppConfig.Fields.inputDirPath));
    Map<String, BeneficiaryData> mbiMap = parseMbiNumbers(path);

    Path outputPath =
        Paths.get(config.stringOption(AppConfig.Fields.outputDirPath).orElse("output"));

    // ResultOfMethodCallIgnored - Don't need to know if it had to be created.
    //noinspection ResultOfMethodCallIgnored
    outputPath.toFile().mkdir();

    String fissOutputFile = config.stringOption(AppConfig.Fields.fissOutputFile).orElse("rda-fiss");
    String mcsOutputFile = config.stringOption(AppConfig.Fields.mcsOutputFile).orElse("rda-mcs");

    try (Sink<MessageOrBuilder> fissSink =
        new NdJsonSink(outputPath.resolve(fissOutputFile + ".ndjson"))) {
      try (Sink<MessageOrBuilder> mcsSink =
          new NdJsonSink(outputPath.resolve(mcsOutputFile + ".ndjson"))) {
        // Sorting the files so tests are more deterministic
        List<String> fissSources = config.stringValues(AppConfig.Fields.fissSources);
        Collections.sort(fissSources);

        for (String fissSource : fissSources) {
          executeTransformation(SourceType.FISS, path, fissSource, mbiMap, fissSink);
        }

        // Sorting the files so tests are more deterministic
        List<String> mcsSources = config.stringValues(AppConfig.Fields.mcsSources);
        Collections.sort(mcsSources);

        for (String mcsSource : mcsSources) {
          executeTransformation(SourceType.MCS, path, mcsSource, mbiMap, mcsSink);
        }
      }
    }
  }

  /**
   * Executes the transformation logic of one source file.
   *
   * @param sourceType The type of claim in the source file.
   * @param path The path to the root directory of the RIF files.
   * @param sourceName The name of the source file to read from.
   * @param mbiMap The generated MBI map to read MBIs values from.
   * @param sink The {@link Sink} used to write out the associated transformed RDA data.
   * @throws IOException If there was a problem accessing any of the files.
   */
  @VisibleForTesting
  void executeTransformation(
      SourceType sourceType,
      Path path,
      String sourceName,
      Map<String, BeneficiaryData> mbiMap,
      Sink<MessageOrBuilder> sink)
      throws IOException {
    try (Parser<String> parser = new RifParser(new RifSource(path.resolve(sourceName + ".csv")))) {
      parser.init();
      int i = 0;

      AbstractTransformer transformer = createTransformer(sourceType, mbiMap);

      while (parser.hasData()) {
        MessageOrBuilder message = transformer.transform(parser.read());
        if (message != null) {
          sink.write(message);
        }

        ++i;
      }

      log.info("Written {} {} claims", i, sourceName);
    }
  }

  /**
   * Creates an {@link AbstractTransformer} for the specific {@link SourceType} of the data being
   * transformed.
   *
   * @param sourceType The {@link SourceType} of the data being transformed. data.
   * @param mbiMap A complete MBI map for looking up MBI values.
   * @return The appropraite {@link AbstractTransformer} implementation for the given {@link
   *     SourceType} of the source file.
   */
  @VisibleForTesting
  AbstractTransformer createTransformer(
      SourceType sourceType, Map<String, BeneficiaryData> mbiMap) {
    if (SourceType.FISS.equals(sourceType)) {
      return new FissTransformer(mbiMap);
    } else {
      return new McsTransformer(mbiMap);
    }
  }

  /**
   * Generates a map of MBI numbers from the given location.
   *
   * @param rootDir Path to the root directory containing the RIF files.
   * @return The completed MBI map.
   */
  @VisibleForTesting
  Map<String, BeneficiaryData> parseMbiNumbers(Path rootDir) throws IOException {
    Map<String, BeneficiaryData> mbiMap = new HashMap<>();

    try (Parser<String> parser =
        new RifParser(new RifSource(rootDir.resolve("beneficiary_history.csv")))) {
      parser.init();

      while (parser.hasData()) {
        Parser.Data<String> data = parser.read();

        data.get(BeneficiaryData.BENE_ID)
            .ifPresent(beneId -> mbiMap.put(beneId, BeneficiaryData.fromData(data)));
      }
    }

    return mbiMap;
  }

  @VisibleForTesting
  static ConfigLoader createYamlConfig(String yamlFilePath) throws FileNotFoundException {
    Yaml yaml = new Yaml();
    AppConfig appConfig = yaml.loadAs(new FileReader(yamlFilePath), AppConfig.class);

    Map<String, Collection<String>> mapConfig =
        ImmutableMap.<String, Collection<String>>builder()
            .put(AppConfig.Fields.inputDirPath, Collections.singleton(appConfig.inputDirPath))
            .put(AppConfig.Fields.outputDirPath, Collections.singleton(appConfig.outputDirPath))
            .put(AppConfig.Fields.fissOutputFile, Collections.singleton(appConfig.fissOutputFile))
            .put(AppConfig.Fields.mcsOutputFile, Collections.singleton(appConfig.mcsOutputFile))
            .put(AppConfig.Fields.fissSources, appConfig.fissSources)
            .put(AppConfig.Fields.mcsSources, appConfig.mcsSources)
            .build();

    return new ConfigLoader(mapConfig::get);
  }

  @VisibleForTesting
  static ConfigLoader createCliConfig(CommandLine cmd) {
    Map<String, Collection<String>> mapConfig =
        ImmutableMap.<String, Collection<String>>builder()
            .put(AppConfig.Fields.inputDirPath, nullOrSet(cmd.getArgList().get(0)))
            .put(AppConfig.Fields.outputDirPath, nullOrSet(cmd.getOptionValue(OUTPUT_FLAG)))
            .put(AppConfig.Fields.fissOutputFile, nullOrSet(cmd.getOptionValue(FISS_OUTPUT_FLAG)))
            .put(AppConfig.Fields.mcsOutputFile, nullOrSet(cmd.getOptionValue(MCS_OUTPUT_FLAG)))
            .put(AppConfig.Fields.fissSources, arrayToSet(cmd.getOptionValues(FISS_FLAG)))
            .put(AppConfig.Fields.mcsSources, arrayToSet(cmd.getOptionValues(MCS_FLAG)))
            .build();

    return new ConfigLoader(mapConfig::get);
  }

  @VisibleForTesting
  static Set<String> nullOrSet(String value) {
    return value == null ? null : Collections.singleton(value);
  }

  @VisibleForTesting
  static Set<String> arrayToSet(String[] values) {
    return new HashSet<>(Arrays.asList(values));
  }

  @VisibleForTesting
  static void printUsage(Options options) {
    final StringWriter stringValue = new StringWriter();
    final PrintWriter writer = new PrintWriter(stringValue);
    HelpFormatter formatter = new HelpFormatter();
    formatter.printUsage(writer, 80, "run_bridge sourceDir", options);
    formatter.printOptions(writer, 80, options, 4, 4);
    writer.flush();
    log.error("Invalid execution \n" + stringValue);
  }

  @VisibleForTesting
  @Data
  @FieldNameConstants
  public static class AppConfig {
    private String inputDirPath;
    private String outputDirPath;
    private String fissOutputFile;
    private String mcsOutputFile;
    private Set<String> fissSources = new HashSet<>();
    private Set<String> mcsSources = new HashSet<>();
  }
}
