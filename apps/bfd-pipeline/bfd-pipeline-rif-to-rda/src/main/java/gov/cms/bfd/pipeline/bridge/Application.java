package gov.cms.bfd.pipeline.bridge;

import com.google.common.annotations.VisibleForTesting;
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
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Application {

  private enum SourceType {
    FISS,
    MCS
  }

  /**
   * Handles translation of a CLI execution, validating and pulling arguments to then invoke the
   * underlying application code with.
   *
   * @param args Array of the command line arguments.
   */
  public static void main(String[] args) {
    try {
      // Build CLI commands for executing this via CLI
      CommandArguments arguments = new CommandArguments("run_bridge.sh");
      arguments
          .register()
          .flag("o")
          .label("-o [outputDir]")
          .description("The directory where the output files will be written to.")
          .argumentCount(1);
      arguments
          .register()
          .argument()
          .label("inputDir")
          .description("The directory containing the files to read from.");
      arguments.addAll(args);

      if (arguments.hasArgs()) {
        String rifRootDir =
            arguments
                .getArg(0)
                .orElseThrow(
                    () ->
                        new IllegalArgumentException(
                            "No root directory for RIF files provided.\n" + arguments.getUsage()));
        String outputDir = arguments.getFlagValue("o").orElse(null);

        new Application().run(rifRootDir, outputDir);
      } else {
        log.error("Invalid execution\n" + arguments.getUsage());
      }
    } catch (IOException e) {
      log.error("Failed to execute", e);
      System.exit(1);
    }
  }

  /**
   * Reads all relevant source files, executing task logic for each claim found.
   *
   * @param rifRootDir The path for the root directory containing the RIF files.
   * @param outputDir The path for the desired RDA output directory.
   * @throws IOException If there was an issue accessing any of the files.
   */
  @VisibleForTesting
  void run(String rifRootDir, String outputDir) throws IOException {
    String[] fissSources = {"inpatient", "outpatient", "home", "hospice", "snf"};
    String[] mcsSources = {"carrier"};

    Path path = Paths.get(rifRootDir);
    Map<String, BeneficiaryData> mbiMap = parseMbiNumbers(path);

    Path outputPath;

    if (outputDir != null) {
      outputPath = Paths.get(outputDir);
    } else {
      outputPath = Paths.get("output");
    }

    // ResultOfMethodCallIgnored - Don't need to know if it had to be created.
    //noinspection ResultOfMethodCallIgnored
    outputPath.toFile().mkdir();

    try (Sink<MessageOrBuilder> fissSink = new NdJsonSink(outputPath.resolve("rda-fiss.ndjson"))) {
      try (Sink<MessageOrBuilder> mcsSink = new NdJsonSink(outputPath.resolve("rda-mcs.ndjson"))) {
        for (String fissSource : fissSources) {
          executeTransformation(SourceType.FISS, path, fissSource, mbiMap, fissSink);
        }

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
        sink.write(message);

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
}
