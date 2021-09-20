package gov.cms.bfd.pipeline.bridge;

import com.google.common.annotations.VisibleForTesting;
import com.google.protobuf.util.JsonFormat;
import gov.cms.bfd.pipeline.bridge.etl.DelimitedStringExtractor;
import gov.cms.bfd.pipeline.bridge.etl.ETLJob;
import gov.cms.bfd.pipeline.bridge.etl.QueueLoader;
import gov.cms.bfd.pipeline.bridge.etl.RifFissTransformer;
import gov.cms.bfd.pipeline.bridge.etl.RifMcsTransformer;
import gov.cms.mpsm.rda.v1.ClaimChange;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Application {

  private static final String DELIMITER = "\\|";

  public static void main(String[] args) {
    new Application().run(args);
  }

  public void run(String[] args) {
    try {
      if (args.length == 1 && !args[0].trim().isEmpty()) {
        run(args[0]);
      } else {
        log.error("Invalid riff root directory provided");
      }
    } catch (IOException e) {
      log.error("Failed to execute", e);
      System.exit(1);
    }
  }

  public void run(String filePath) throws IOException {
    String[] fissSources = {"inpatient", "outpatient", "home", "hospice", "snf"};
    String[] mcsSources = {"carrier"};

    Path path = Paths.get(filePath);
    Map<String, String> mbiMap = getMbiNumbers(path);

    Path outputPath = Paths.get("output");
    outputPath.toFile().mkdir();

    try (BufferedWriter fissWriter =
        new BufferedWriter(new FileWriter(outputPath.resolve("rda_fiss.ndjson").toString()))) {
      try (BufferedWriter mcsWriter =
          new BufferedWriter(new FileWriter(outputPath.resolve("rda_mcs.ndjson").toString()))) {
        for (String fissSource : fissSources) {
          createEtlJob("fiss", path, fissSource, mbiMap, fissWriter);
        }

        for (String mcsSource : mcsSources) {
          createEtlJob("mcs", path, mcsSource, mbiMap, mcsWriter);
        }
      }
    }
  }

  @VisibleForTesting
  void createEtlJob(
      String sourceType,
      Path path,
      String sourceName,
      Map<String, String> mbiMap,
      BufferedWriter writer)
      throws IOException {
    try (BufferedReader rifFileReader =
        Files.newBufferedReader(path.resolve(sourceName + ".csv"), StandardCharsets.UTF_8)) {
      String line;

      if ((line = rifFileReader.readLine()) != null) {
        Map<String, Integer> headerIndexMap = createHeaderIndexMap(line, DELIMITER);

        ETLJob.Transformer<String[], ClaimChange> transformer =
            createTransformer(sourceType, headerIndexMap, mbiMap);
        ETLJob.Loader<ClaimChange> loader =
            new QueueLoader<>(
                writer,
                claimChange ->
                    JsonFormat.printer().omittingInsignificantWhitespace().print(claimChange));

        int i = 0;

        while ((line = rifFileReader.readLine()) != null) {
          new ETLJob<>(new DelimitedStringExtractor(line, DELIMITER), transformer, loader).run();

          if (i % 100 == 0) {
            System.out.printf("\rWritten %s %s claims", i, sourceName);
          }

          ++i;
        }

        System.out.printf("\rWritten %s %s claims\n", i, sourceName);
      }
    }
  }

  @VisibleForTesting
  ETLJob.Transformer<String[], ClaimChange> createTransformer(
      String type, Map<String, Integer> headerIndexMap, Map<String, String> mbiMap) {
    if (type.equals("fiss")) {
      return new RifFissTransformer(headerIndexMap, mbiMap);
    } else {
      return new RifMcsTransformer(headerIndexMap, mbiMap);
    }
  }

  Map<String, String> getMbiNumbers(Path rootDir) {
    Map<String, String> mbiMap = new HashMap<>();

    try (BufferedReader bufferedReader =
        Files.newBufferedReader(
            rootDir.resolve("beneficiary_history.csv"), StandardCharsets.UTF_8)) {
      int mbiColumnIndex = 0;

      String line = bufferedReader.readLine();
      String[] headers = line.split(DELIMITER, 0);
      while (mbiColumnIndex < headers.length
          && !headers[mbiColumnIndex].equalsIgnoreCase("mbi_num")) ++mbiColumnIndex;

      if (mbiColumnIndex >= headers.length) {
        throw new IllegalStateException("MBI_NUM filed not foudn in file");
      }

      while ((line = bufferedReader.readLine()) != null) {
        String[] rowData = line.split(DELIMITER, 0);
        mbiMap.put(rowData[1], rowData[mbiColumnIndex]);
      }
    } catch (IOException e) {
      throw new IllegalStateException("Failed to build mbi map", e);
    }

    return mbiMap;
  }

  @VisibleForTesting
  Map<String, Integer> createHeaderIndexMap(String line, String delimiterRegEx) {
    String[] headers = line.split(delimiterRegEx);
    Map<String, Integer> headerIndexMap = new HashMap<>(headers.length);

    for (int i = 0; i < headers.length; ++i) {
      headerIndexMap.put(headers[i], i);
    }

    return headerIndexMap;
  }
}
