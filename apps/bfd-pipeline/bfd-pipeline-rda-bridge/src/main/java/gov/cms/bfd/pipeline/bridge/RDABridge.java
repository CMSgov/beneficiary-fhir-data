package gov.cms.bfd.pipeline.bridge;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
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
import gov.cms.bfd.sharedutils.interfaces.ThrowingFunction;
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
import org.apache.commons.io.FilenameUtils;

@Slf4j
public class RDABridge {

  enum SourceType {
    FISS,
    MCS
  }

  private static final String OUTPUT_FLAG = "o";
  private static final String MBI_FLAG = "b";
  private static final String FISS_FLAG = "f";
  private static final String MCS_FLAG = "m";
  private static final String FISS_OUTPUT_FLAG = "g";
  private static final String MCS_OUTPUT_FLAG = "n";
  private static final String EXTERNAL_CONFIG_FLAG = "e";

  private static final Map<String, ThrowingFunction<Parser<String>, Path, IOException>> parserMap =
      Map.of("csv", filePath -> new RifParser(new RifSource(filePath)));

  private static final Map<String, ThrowingFunction<Sink<MessageOrBuilder>, Path, IOException>>
      sinkMap = Map.of("ndjson", NdJsonSink::new);

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
              .addOption(MBI_FLAG, true, "Benefit History file to read from")
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
    Map<String, BeneficiaryData> mbiMap =
        parseMbiNumbers(path.resolve(config.stringValue(AppConfig.Fields.mbiSource)));

    Path outputPath =
        Paths.get(config.stringOption(AppConfig.Fields.outputDirPath).orElse("output"));

    // ResultOfMethodCallIgnored - Don't need to know if it had to be created.
    //noinspection ResultOfMethodCallIgnored
    outputPath.toFile().mkdir();

    String fissOutputFile =
        config.stringOption(AppConfig.Fields.fissOutputFile).orElse("rda-fiss.ndjson");
    String mcsOutputFile =
        config.stringOption(AppConfig.Fields.mcsOutputFile).orElse("rda-mcs.ndjson");

    Path fissOutputPath = outputPath.resolve(fissOutputFile);
    Path mcsOutputPath = outputPath.resolve(mcsOutputFile);

    String fissOutputType = FilenameUtils.getExtension(fissOutputPath.getFileName().toString());
    String mcsOutputType = FilenameUtils.getExtension(mcsOutputPath.getFileName().toString());

    if (!sinkMap.containsKey(fissOutputType)) {
      throw new IllegalArgumentException(
          "Unsupported fiss output file type '" + fissOutputType + "'");
    } else if (!sinkMap.containsKey(mcsOutputType)) {
      throw new IllegalArgumentException(
          "Unsupported mcs output file type '" + mcsOutputType + "'");
    } else {
      try (Sink<MessageOrBuilder> fissSink = sinkMap.get(fissOutputType).apply(fissOutputPath);
          Sink<MessageOrBuilder> mcsSink = sinkMap.get(mcsOutputType).apply(mcsOutputPath)) {
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
    Path file = path.resolve(sourceName);
    String fileType = FilenameUtils.getExtension(file.getFileName().toString());

    if (parserMap.containsKey(fileType)) {
      try (Parser<String> parser = parserMap.get(fileType).apply(file)) {
        parser.init();
        int claimsWritten = 0;

        AbstractTransformer transformer = createTransformer(sourceType, mbiMap);

        while (parser.hasData()) {
          MessageOrBuilder message = transformer.transform(parser.read());
          if (message != null) {
            sink.write(message);
          }

          ++claimsWritten;
        }

        log.info("Wrote {} {} claims", claimsWritten, sourceName);
      }
    } else {
      throw new IllegalArgumentException("No support for parsing files of type '" + fileType + "'");
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
    if (SourceType.FISS == sourceType) {
      return new FissTransformer(mbiMap);
    } else {
      return new McsTransformer(mbiMap);
    }
  }

  /**
   * Generates a map of MBI numbers from the given location.
   *
   * @param filePath Path to the root directory containing the RIF files.
   * @return The completed MBI map.
   */
  @VisibleForTesting
  Map<String, BeneficiaryData> parseMbiNumbers(Path filePath) throws IOException {
    Map<String, BeneficiaryData> mbiMap = new HashMap<>();

    String fileType = FilenameUtils.getExtension(filePath.getFileName().toString());

    if (parserMap.containsKey(fileType)) {
      try (Parser<String> parser = parserMap.get(fileType).apply(filePath)) {
        parser.init();

        while (parser.hasData()) {
          Parser.Data<String> data = parser.read();

          data.get(BeneficiaryData.BENE_ID)
              .ifPresent(beneId -> mbiMap.put(beneId, BeneficiaryData.fromData(data)));
        }
      }
    } else {
      throw new IllegalArgumentException("No support for parsing files of type '" + fileType + "'");
    }

    return mbiMap;
  }

  /**
   * Creates a {@link ConfigLoader} from a given yaml configuration file.
   *
   * @param yamlFilePath Path to the yaml configuration file.
   * @return The {@link ConfigLoader} generated from the yaml configuration file.
   * @throws FileNotFoundException If the yaml configuration file was not found.
   */
  @VisibleForTesting
  static ConfigLoader createYamlConfig(String yamlFilePath) throws IOException {
    try (FileReader reader = new FileReader(yamlFilePath)) {
      ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
      AppConfig appConfig = mapper.readValue(reader, AppConfig.class);

      Map<String, Collection<String>> mapConfig =
          ImmutableMap.<String, Collection<String>>builder()
              .put(AppConfig.Fields.inputDirPath, Collections.singleton(appConfig.inputDirPath))
              .put(AppConfig.Fields.outputDirPath, Collections.singleton(appConfig.outputDirPath))
              .put(AppConfig.Fields.fissOutputFile, Collections.singleton(appConfig.fissOutputFile))
              .put(AppConfig.Fields.mcsOutputFile, Collections.singleton(appConfig.mcsOutputFile))
              .put(AppConfig.Fields.fissSources, appConfig.fissSources)
              .put(AppConfig.Fields.mcsSources, appConfig.mcsSources)
              .put(AppConfig.Fields.mbiSource, Collections.singleton(appConfig.mbiSource))
              .build();

      return new ConfigLoader(mapConfig::get);
    }
  }

  /**
   * Creates a {@link ConfigLoader} from the given command line arguments.
   *
   * @param cmd {@link CommandLine} containing the arguments/options used with the CLI.
   * @return The {@link ConfigLoader} generated from the CLI arguments/options.
   */
  @VisibleForTesting
  static ConfigLoader createCliConfig(CommandLine cmd) {
    ImmutableMap.Builder<String, Collection<String>> builder = ImmutableMap.builder();

    putIfNotNull(builder, AppConfig.Fields.inputDirPath, cmd.getArgList().get(0));
    putIfNotNull(builder, AppConfig.Fields.outputDirPath, cmd.getOptionValue(OUTPUT_FLAG));
    putIfNotNull(builder, AppConfig.Fields.fissOutputFile, cmd.getOptionValue(FISS_OUTPUT_FLAG));
    putIfNotNull(builder, AppConfig.Fields.mcsOutputFile, cmd.getOptionValue(MCS_OUTPUT_FLAG));
    putIfNotNull(builder, AppConfig.Fields.fissSources, cmd.getOptionValues(FISS_FLAG));
    putIfNotNull(builder, AppConfig.Fields.mcsSources, cmd.getOptionValues(MCS_FLAG));
    putIfNotNull(builder, AppConfig.Fields.mbiSource, cmd.getOptionValue(MBI_FLAG));

    ImmutableMap<String, Collection<String>> mapConfig = builder.build();

    return new ConfigLoader(mapConfig::get);
  }

  @VisibleForTesting
  static void putIfNotNull(
      ImmutableMap.Builder<String, Collection<String>> builder, String key, String value) {
    if (value != null) {
      builder.put(key, Collections.singleton(value));
    }
  }

  @VisibleForTesting
  static void putIfNotNull(
      ImmutableMap.Builder<String, Collection<String>> builder, String key, String[] values) {
    if (values != null && values.length > 0) {
      builder.put(key, new HashSet<>(Arrays.asList(values)));
    }
  }

  /**
   * Helper method to print the usage message for the CLI tool.
   *
   * @param options The {@link Options} to generate the usage message from.
   */
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

  /** Helper class for defining application specific configurations. */
  @VisibleForTesting
  @Data
  @FieldNameConstants
  public static class AppConfig {
    private String inputDirPath;
    private String outputDirPath;
    private String fissOutputFile;
    private String mcsOutputFile;
    private String mbiSource;
    private Set<String> fissSources = new HashSet<>();
    private Set<String> mcsSources = new HashSet<>();
  }
}
