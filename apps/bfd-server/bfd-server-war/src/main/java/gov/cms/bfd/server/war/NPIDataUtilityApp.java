package gov.cms.bfd.server.war;

import com.google.common.base.Strings;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
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
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A simple application that downloads NPI Data file; unzips it and then converts it to UTF-8
 * format.
 */
public final class NPIDataUtilityApp {
  private static final Logger LOGGER = LoggerFactory.getLogger(NPIDataUtilityApp.class);
  /**
   * The name of the classpath resource (for the project's main web application) for the FDA
   * "Products" TSV file.
   */
  public static final String NPI_RESOURCE = "npi_or.tsv";

  /** Size of the buffer to read/write data */
  private static final int BUFFER_SIZE = 4096;

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
   *
   * @throws IOException if there is an issue creating or iterating over the downloaded files
   */
  public static void main(String[] args) throws IOException {
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
    Path workingDir = Files.createTempDirectory("npi-data");

    // If the output file isn't already there, go build it.
    Path convertedNpiDataFile = outputPath.resolve(NPI_RESOURCE);
    if (!Files.exists(convertedNpiDataFile)) {
      String fileName = getFileName(false);
      try {
        try {
          buildNPIResource(convertedNpiDataFile, workingDir, fileName);
        } catch (FileNotFoundException ex) {
          try {
            fileName = getFileName(true);
            buildNPIResource(convertedNpiDataFile, workingDir, fileName);
          } catch (Exception e) {

          }
        } finally {
          // Recursively delete the working dir.
          Files.walk(workingDir)
              .sorted(Comparator.reverseOrder())
              .map(Path::toFile)
              .peek(System.out::println)
              .forEach(File::delete);
        }
      } catch (Exception exception) {
        LOGGER.error("NPI data file could not be read.  Error:", exception);
      }
    }
  }

  /**
   * Creates the {@link #NPI_RESOURCE} file in the specified location.
   *
   * @param convertedNpiDataFile the output file/resource to produce
   * @param workingDir a directory that temporary/working files can be written to
   * @throws IOException (any errors encountered will be bubbled up)
   */
  private static void buildNPIResource(Path convertedNpiDataFile, Path workingDir, String fileName)
      throws Exception {
    // download NPI file
    Path downloadedNpiZipFile =
        Paths.get(workingDir.resolve("npidata.zip").toFile().getAbsolutePath());
    URL ndctextZipUrl = new URL(fileName);
    if (!Files.isReadable(downloadedNpiZipFile)) {
      // connectionTimeout, readTimeout = 10 seconds
      FileUtils.copyURLToFile(
          ndctextZipUrl, new File(downloadedNpiZipFile.toFile().getAbsolutePath()), 10000, 10000);
    }

    // unzip NPI file
    unzip(downloadedNpiZipFile, workingDir);
    File f = new File(workingDir.toString());
    File[] matchingFiles =
        f.listFiles(
            new FilenameFilter() {
              public boolean accept(File dir, String name) {
                return name.startsWith("npidata_pfile_") && !name.endsWith("_FileHeader.csv");
              }
            });

    Path originalNpiDataFile = workingDir.resolve(matchingFiles[0].getName());
    if (!Files.isReadable(originalNpiDataFile))
      throw new IllegalStateException("Unable to locate npidata_pfile in " + ndctextZipUrl);

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

      String provider = "";
      for (String in; (in = reader.readLine()) != null; ) {
        String[] inputLine = in.split(",");
        String orgData = inputLine[4].trim();
        if (!Strings.isNullOrEmpty(orgData) && !"\"\"".equals(orgData)) {
          provider = orgData;
        }

        String output = inputLine[0].replace("\"", "") + "\t" + provider.replace("\"", "");

        out.write(output);
        out.newLine();
      }
    }
  }

  /**
   * Extracts a zip file specified by the zipFilePath to a directory specified by destDirectory
   * (will be created if does not exists)
   *
   * @param zipFilePath
   * @param destDirectory
   * @throws IOException
   */
  private static void unzip(Path zipFilePath, Path destDirectory) throws IOException {
    ZipInputStream zipIn =
        new ZipInputStream(new FileInputStream(zipFilePath.toFile().getAbsolutePath()));
    ZipEntry entry = zipIn.getNextEntry();
    // iterates over entries in the zip file
    while (entry != null) {
      Path filePath = Paths.get(destDirectory.toFile().getAbsolutePath(), entry.getName());
      if (!entry.isDirectory()) {
        // if the entry is a file, extracts it
        extractFile(zipIn, filePath);
      } else {
        // if the entry is a directory, make the directory
        File dir = new File(filePath.toFile().getAbsolutePath());
        dir.mkdir();
      }
      zipIn.closeEntry();
      entry = zipIn.getNextEntry();
    }
    zipIn.close();
  }

  /**
   * Extracts a zip entry (file entry)
   *
   * @param zipIn
   * @param filePath
   * @throws IOException
   */
  private static void extractFile(ZipInputStream zipIn, Path filePath) throws IOException {
    BufferedOutputStream bos =
        new BufferedOutputStream(new FileOutputStream(filePath.toFile().getAbsolutePath()));
    byte[] bytesIn = new byte[BUFFER_SIZE];
    int read = 0;
    while ((read = zipIn.read(bytesIn)) != -1) {
      bos.write(bytesIn, 0, read);
    }
    bos.close();
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
