package gov.cms.bfd.pipeline.bridge;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Sets;
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
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.ObjectUtils;

@Slf4j
public class RDABridge {

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
      Options options =
          new Options()
              .addOption("o", true, "The directory where the output files will be written to.")
              .addOption("f", true, "FISS file to read from")
              .addOption("m", true, "MCS file to read from");

      CommandLineParser parser = new DefaultParser();
      CommandLine cmd = parser.parse(options, args);

      if (!cmd.getArgList().isEmpty()) {
        String rifRootDir = cmd.getArgList().get(0);
        String outputDir = ObjectUtils.defaultIfNull(cmd.getOptionValue("o"), "output");
        Set<String> fissFiles = Sets.newHashSet(cmd.getOptionValues('f'));
        Set<String> mcsFiles = Sets.newHashSet(cmd.getOptionValues('m'));

        new RDABridge().run(new GenConfig(rifRootDir, outputDir, fissFiles, mcsFiles));
      } else {
        log.error("Invalid execution");
        final PrintWriter writer = new PrintWriter(System.out);
        HelpFormatter formatter = new HelpFormatter();
        formatter.printUsage(writer, 80, "run_bridge sourceDir", options);
        formatter.printOptions(writer, 80, options, 4, 4);
        writer.flush();
      }
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
  public void run(GenConfig config) throws IOException {
    Path path = Paths.get(config.getInputDir());
    Map<String, BeneficiaryData> mbiMap = parseMbiNumbers(path);

    Path outputPath = Paths.get(config.getOutputDir());

    // ResultOfMethodCallIgnored - Don't need to know if it had to be created.
    //noinspection ResultOfMethodCallIgnored
    outputPath.toFile().mkdir();

    try (Sink<MessageOrBuilder> fissSink = new NdJsonSink(outputPath.resolve("rda-fiss.ndjson"))) {
      try (Sink<MessageOrBuilder> mcsSink = new NdJsonSink(outputPath.resolve("rda-mcs.ndjson"))) {
        // Sorting the files so tests are more deterministic
        List<String> fissSources = new ArrayList<>(config.getFissFiles());
        Collections.sort(fissSources);

        for (String fissSource : fissSources) {
          executeTransformation(SourceType.FISS, path, fissSource, mbiMap, fissSink);
        }

        // Sorting the files so tests are more deterministic
        List<String> mcsSources = new ArrayList<>(config.getMcsFiles());
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

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class GenConfig {

    private String inputDir;
    private String outputDir;
    private Set<String> fissFiles;
    private Set<String> mcsFiles;
  }
}
