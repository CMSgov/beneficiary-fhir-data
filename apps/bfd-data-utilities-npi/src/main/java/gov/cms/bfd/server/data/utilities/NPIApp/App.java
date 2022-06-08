package gov.cms.bfd.server.data.utilities.NPIApp;

import com.google.common.base.Strings;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Calendar;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A simple application that downloads NPI Data file; unzips it and then converts it to UTF-8
 * format.
 */
public final class App {
  private static final Logger LOGGER = LoggerFactory.getLogger(App.class);
  /**
   * The name of the classpath resource (for the project's main web application) for the FDA
   * "Products" TSV file.
   */
  public static final String NPI_RESOURCE = "npi_or.tsv";

  /**
   * The application entry point, which will receive all non-JVM command line options in the <code>
   * args</code> array.
   *
   * @param args
   *     <p>The non-JVM command line arguments that the application was launched with. Must include:
   *     <ol>
   *       <li><code>OUTPUT_DIR</code>: the first (and only) argument for this application, which
   *           should be the path to the project's <code>${project.build.outputDirectory}</code>
   *           directory (i.e. <code>target/classes/</code>)
   *     </ol>
   */
  public static void main(String[] args) {
    if (args.length < 1) {
      System.err.println("OUTPUT_DIR argument not specified for NPI download.");
      System.exit(1);
    }
    if (args.length > 1) {
      System.err.println("Invalid arguments supplied for NPI download.");
      System.exit(2);
    }

    Path outputPath = Paths.get(args[0]);
    if (!Files.isDirectory(outputPath)) {
      System.err.println("OUTPUT_DIR does not exist for NPI download.");
      System.exit(3);
    }

    // Create a temp directory that will be recursively deleted when we're done.
    Path tempDir = null;
    try {
      tempDir = Files.createTempDirectory("npi-data");
    } catch (IOException e) {
      System.err.println("Cannot create temporary directory.");
      // System.exit(4);
    }

    // If the output file isn't already there, go build it.
    Path convertedNpiDataFile = outputPath.resolve(NPI_RESOURCE);
    if (!Files.exists(convertedNpiDataFile)) {
      try {
        buildNPIResource(convertedNpiDataFile, tempDir);
      } catch (IOException exception) {
        LOGGER.error("NPI data file could not be read.  Error:", exception);
        // System.exit(5);
      } finally {
        recursivelyDelete(tempDir);
      }
    }
  }

  /** @param tempDir */
  private static void recursivelyDelete(Path tempDir) {
    // Recursively delete the working dir.
    try {
      Files.walk(tempDir)
          .sorted(Comparator.reverseOrder())
          .map(Path::toFile)
          .peek(System.out::println)
          .forEach(File::delete);
    } catch (IOException e) {
      LOGGER.warn("Failed to cleanup the temporary folder", e);
    }
  }

  /**
   * Creates the {@link #NPI_RESOURCE} file in the specified location.
   *
   * @param convertedNpiDataFile the output file/resource to produce
   * @param workingDir a directory that temporary/working files can be written to
   * @throws IOException (any errors encountered will be bubbled up)
   */
  private static void buildNPIResource(Path convertedNpiDataFile, Path workingDir)
      throws IOException {
    Path originalNpiDataFile;
    String fileName;

    try {
      fileName = getFileName(false);
      originalNpiDataFile = DataUtilityCommons.getOriginalNpiDataFile(workingDir, fileName);
    } catch (IOException e) {
      fileName = getFileName(true);
      originalNpiDataFile = DataUtilityCommons.getOriginalNpiDataFile(workingDir, fileName);
    }

    // TODO: Need a check here for still not found

    convertNpiDataFile(convertedNpiDataFile, originalNpiDataFile);
  }

  /**
   * @param convertedNpiDataFile
   * @param originalNpiDataFile
   * @throws IOException
   */
  private static void convertNpiDataFile(Path convertedNpiDataFile, Path originalNpiDataFile)
      throws IOException {
    // convert file format from cp1252 to utf8
    CharsetDecoder inDec =
        Charset.forName("windows-1252")
            .newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT);

    CharsetEncoder outEnc =
        StandardCharsets.UTF_8
            .newEncoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT);

    try (FileInputStream is = new FileInputStream(originalNpiDataFile.toFile().getAbsolutePath());
        BufferedReader reader = new BufferedReader(new InputStreamReader(is, inDec));
        FileOutputStream fw =
            new FileOutputStream(convertedNpiDataFile.toFile().getAbsolutePath());
        BufferedWriter out = new BufferedWriter(new OutputStreamWriter(fw, outEnc))) {

      // TODO: Explain file format and show example
      // TODO: Extract this logic into a testable method or class
      String lastNonEmptyOrgName = null;
      for (String line; (line = reader.readLine()) != null; ) {
        String[] fields = line.split(",");

        String orgName = fields[4].trim().replace("\"", "");
        String npi = fields[0].replace("\"", "");

        if (!Strings.isNullOrEmpty(orgName)) {
          lastNonEmptyOrgName = orgName;
        }

        if (lastNonEmptyOrgName == null) {
          LOGGER.warn("Skipping a record due to unavailable organization name: '" + line + "'");
          continue;
        }

        out.write(npi + "\t" + lastNonEmptyOrgName);
        out.newLine();
      }
    }
  }

  /**
   * Extracts a file name
   *
   * @param getMonthBefore
   * @return a file name string
   */
  private static String getFileName(boolean getMonthBefore) {
    Map<Integer, String> months =
        new HashMap<Integer, String>() {
          {
            put(0, "January");
            put(1, "February");
            put(2, "March");
            put(3, "April");
            put(4, "May");
            put(5, "June");
            put(6, "July");
            put(7, "August");
            put(8, "September");
            put(9, "October");
            put(10, "November");
            put(11, "December");
          }
        };

    Calendar cal = Calendar.getInstance();
    int month = cal.get(Calendar.MONTH);
    int currentYear = cal.get(Calendar.YEAR);
    String currentMonth = null;

    String fileName = null;

    if (getMonthBefore) {
      if (month == 0) {
        currentMonth = months.get(11);
        currentYear = currentYear - 1;
      } else {
        currentMonth = months.get(month - 1);
      }

    } else {
      currentMonth = months.get(month);
    }

    fileName =
        "https://download.cms.gov/nppes/NPPES_Data_Dissemination_"
            + currentMonth
            + "_"
            + currentYear
            + ".zip";

    return fileName;
  }
}

