package  gov.cms.bfd.server.data.utilities.NPIApp;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
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
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Calendar;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import com.google.common.base.Strings;

public class DataUtilityCommons {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataUtilityCommons.class);

   /** Size of the buffer to read/write data */
  private static final int BUFFER_SIZE = 4096;

/**
   * Gets the org names from the npi file
   *
   * @param outputDir the output directory
   * @param npiFile the npi file
   */
public static void getNPIOrgNames(String outputDir, String npiFile){
  Path outputPath = Paths.get(outputDir);
    if (!Files.isDirectory(outputPath)) {
      throw new IllegalStateException("OUTPUT_DIR does not exist for NPI download.");
    }

    // Create a temp directory that will be recursively deleted when we're done.
    Path tempDir = null;
    try {
      tempDir = Files.createTempDirectory("npi-data");
    } catch (IOException e) {
      throw new IllegalStateException("Cannot create temporary directory.");
    }

    // If the output file isn't already there, go build it.
    Path convertedNpiDataFile = outputPath.resolve(npiFile);
    if (!Files.exists(convertedNpiDataFile)) {
      try {
        buildNPIResource(convertedNpiDataFile, tempDir);
      } catch (IOException exception) {
        LOGGER.error("NPI data file could not be read.  Error:", exception);
      } finally {
        recursivelyDelete(tempDir);
      }
    }
}

  /**
   * Extracts a zip file specified by the zipFilePath to a directory specified by destDirectory
   * (will be created if does not exists)
   *
   * @param zipFilePath the zip file path
   * @param destDirectory the destination directory
   * @throws IOException (any errors encountered will be bubbled up)
   */
  public static void unzip(Path zipFilePath, Path destDirectory) throws IOException {
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
   * @param zipIn the zip file coming in
   * @param filePath the file path for the file
   * @throws IOException (any errors encountered will be bubbled up)
   */
  public static void extractFile(ZipInputStream zipIn, Path filePath) throws IOException {
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
   * Creates the file in the specified location.
   *
   * @param workingDir a directory that temporary/working files can be written to
   * @param fileName the output file/resource to produce
   * @throws IOException (any errors encountered will be bubbled up)
   * @return path to file
   */
  public static Path getOriginalNpiDataFile(Path workingDir, String fileName) throws IOException {
    // download NPI file
    Path downloadedNpiZipFile =
        Paths.get(workingDir.resolve("npidata.zip").toFile().getAbsolutePath());
    URL ndctextZipUrl = new URL(fileName);
    if (!Files.isReadable(downloadedNpiZipFile)) {
      // connectionTimeout, readTimeout = 10 seconds
      FileUtils.copyURLToFile(
          ndctextZipUrl, new File(downloadedNpiZipFile.toFile().getAbsolutePath()), 100000, 100000);
    }

    // unzip NPI file.  Zip file contains these files
    // pl_pfile_20050523-20220410.csv
    // pl_pfile_20050523-20220410_FileHeader.csv
    // othername_pfile_20050523-20220410.csv
    // othername_pfile_20050523-20220410_FileHeader.csv
    // NPPES_Data_Dissemination_Readme.pdf
    // NPPES_Data_Dissemination_CodeValues.pdf
    // npidata_pfile_20050523-20220410.csv
    // npidata_pfile_20050523-20220410_FileHeader.csv
    // endpoint_pfile_20050523-20220410.csv
    // endpoint_pfile_20050523-20220410_FileHeader.csv
    unzip(downloadedNpiZipFile, workingDir);
    File f = new File(workingDir.toString());
    File[] matchingFiles =
        f.listFiles(
            new FilenameFilter() {
              public boolean accept(File dir, String name) {
                return name.startsWith("npidata_pfile_") && !name.endsWith("_FileHeader.csv");
              }
            });

    if (matchingFiles.length > 1) {
      throw new IllegalStateException("More than one NPI file found");
    }

    Path originalNpiDataFile = workingDir.resolve(matchingFiles[0].getName());
    if (!Files.isReadable(originalNpiDataFile))
      throw new IllegalStateException("Unable to locate npidata_pfile in " + ndctextZipUrl);
    return originalNpiDataFile;
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
      originalNpiDataFile = getOriginalNpiDataFile(workingDir, fileName);
    } catch (IOException e) {
      fileName = getFileName(true);
      originalNpiDataFile = getOriginalNpiDataFile(workingDir, fileName);
    }
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
      String firstLine = reader.readLine();
       String[] fields = firstLine.split(",");

        String orgName = fields[4].trim().replace("\"", "");
        String npi = fields[0].replace("\"", "");
        out.write(npi + "\t" + orgName);
        out.newLine();
        orgName = null;
        npi = null;
        String lastNonEmptyOrgName = null;

      for (String line; (line = reader.readLine()) != null; ) {
        fields = line.split(",");

        orgName = fields[4].trim().replace("\"", "");
        npi = fields[0].replace("\"", "");

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
