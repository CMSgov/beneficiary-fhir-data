package gov.cms.bfd.data.npi.utility;

import com.google.common.base.Strings;
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
import java.util.Calendar;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Data Utility Commons class for npi. * */
public class DataUtilityCommons {
  private static final Logger LOGGER = LoggerFactory.getLogger(DataUtilityCommons.class);

  /** Size of the buffer to read/write data. */
  private static final int BUFFER_SIZE = 4096;

  /** The day of the month we should check to see if the file has posted. */
  private static final int DAYS_IN_EXPIRATION = 10;

  /**
   * Gets the org names from the npi file.
   *
   * @param outputDir the output directory
   * @param downloadUrl the downloadUrl passed in
   * @param npiFile the npi file
   * @throws IOException if there is an issue reading file
   * @throws IllegalStateException if there is an issue with the output directory
   */
  public static void getNPIOrgNames(String outputDir, Optional<String> downloadUrl, String npiFile)
      throws IOException, IllegalStateException {
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
    try {
      buildNPIResource(convertedNpiDataFile, downloadUrl, tempDir);
    } catch (IOException exception) {
      LOGGER.error("NPI data file could not be read.  Error:", exception);
      throw new IOException(exception);
    } finally {
      recursivelyDelete(tempDir);
    }
  }

  /**
   * Extracts a zip file specified by the zipFilePath to a directory specified by destDirectory
   * (will be created if does not exists).
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
   * Extracts a zip entry (file entry).
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
   * @throws IllegalStateException if there is an issue with NPI File
   * @return path to file
   */
  public static Path getOriginalNpiDataFile(Path workingDir, String fileName)
      throws IOException, IllegalStateException {
    // download NPI file
    Path downloadedNpiZipFile =
        Paths.get(workingDir.resolve("npidata.zip").toFile().getAbsolutePath());
    URL ndctextZipUrl = new URL(fileName);
    if (!Files.isReadable(downloadedNpiZipFile)) {
      // connectionTimeout, readTimeout = 10 seconds
      final Integer connectionTimeout = 100000;
      final Integer readTimeout = 100000;

      FileUtils.copyURLToFile(
          ndctextZipUrl,
          new File(downloadedNpiZipFile.toFile().getAbsolutePath()),
          connectionTimeout,
          readTimeout);
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
                return name.startsWith("npidata_pfile_")
                    && !name.endsWith("_FileHeader.csv")
                    && !name.endsWith("_fileheader.csv");
              }
            });

    if (matchingFiles.length == 0) {
      throw new IllegalStateException("No NPI file found");
    }

    if (matchingFiles.length > 1) {
      throw new IllegalStateException("More than one NPI file found");
    }

    Path originalNpiDataFile = workingDir.resolve(matchingFiles[0].getName());
    if (!Files.isReadable(originalNpiDataFile))
      throw new IllegalStateException("Unable to locate npidata_pfile in " + ndctextZipUrl);
    return originalNpiDataFile;
  }

  /**
   * Deletes the directory.
   *
   * @param tempDir for the temp directory
   */
  private static void recursivelyDelete(Path tempDir) {
    // Recursively delete the working dir.
    try {
      Files.walk(tempDir)
          .sorted(Comparator.reverseOrder())
          .map(Path::toFile)
          .peek(f -> LOGGER.info("deleting {}", f))
          .forEach(File::delete);
    } catch (IOException e) {
      LOGGER.warn("Failed to cleanup the temporary folder", e);
    }
  }

  /**
   * Creates the {@link #NPI_RESOURCE} file in the specified location.
   *
   * @param convertedNpiDataFile the output file/resource to produce
   * @param downloadUrl is the download url
   * @param workingDir a directory that temporary/working files can be written to
   * @throws IOException (any errors encountered will be bubbled up)
   */
  private static void buildNPIResource(
      Path convertedNpiDataFile, Optional<String> downloadUrl, Path workingDir) throws IOException {
    Path originalNpiDataFile;
    String fileUrl;

    // don't recreate the file if it already exists
    if (Files.isRegularFile(convertedNpiDataFile)) {
      LOGGER.info(
          "using existing data file {}, delete file if it is broken and re-run",
          convertedNpiDataFile);
      return;
    }

    if (downloadUrl.isPresent()) {
      try {
        originalNpiDataFile = getOriginalNpiDataFile(workingDir, downloadUrl.get());
        convertNpiDataFile(convertedNpiDataFile, originalNpiDataFile);
      } catch (IOException e) {
        Calendar cal = Calendar.getInstance();
        int day = cal.get(Calendar.DAY_OF_MONTH);
        if (day >= DAYS_IN_EXPIRATION) {
          throw new IOException(
              "NPI file is not available for the month and should have been made available by the NPI Files team.");
        } else {
          throw new IOException("NPI file is not for the month.");
        }
      }

    } else {
      try {
        fileUrl = FileNameCalculation.getFileName(false);
        originalNpiDataFile = getOriginalNpiDataFile(workingDir, fileUrl);
      } catch (IOException e) {
        fileUrl = FileNameCalculation.getFileName(true);
        originalNpiDataFile = getOriginalNpiDataFile(workingDir, fileUrl);
      }
      convertNpiDataFile(convertedNpiDataFile, originalNpiDataFile);
    }
  }

  /**
   * Converts the npi data file.
   *
   * @param convertedNpiDataFile converted npi data file.
   * @param originalNpiDataFile original npi data file.
   * @throws IOException exception thrown.
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

      // Get indexes according to header
      String line = reader.readLine();
      String[] fields = line.split(",");
      Map<String, Integer> indexes = getIndexNumbers(fields);
      Integer npiIndex = getIndexNumberForField(indexes, "NPI");
      Integer entityTypeCodeIndex = getIndexNumberForField(indexes, "Entity Type Code");
      Integer orgNameIndex =
          getIndexNumberForField(indexes, "Provider Organization Name (Legal Business Name)");

      while ((line = reader.readLine()) != null) {
        fields = line.split(",");

        String orgName = fields[orgNameIndex].trim().replace("\"", "");
        String npi = fields[npiIndex].trim().replace("\"", "");
        String entityTypeCode = fields[entityTypeCodeIndex].trim().replace("\"", "");

        // entity type code 2 is organization
        if (!Strings.isNullOrEmpty(entityTypeCode) && Integer.parseInt(entityTypeCode) == 2) {
          out.write(npi + "\t" + orgName);
          out.newLine();
        }
      }
    }
  }

  /**
   * sets the index number to their header value.
   *
   * @param fields the string array of header values.
   * @return map of indexes and field names
   */
  protected static Map<String, Integer> getIndexNumbers(String[] fields) {
    Map<String, Integer> indexNumbers = new HashMap<String, Integer>();
    Integer indexCounter = 0;
    for (String field : fields) {
      indexNumbers.put(field.trim().replace("\"", ""), indexCounter++);
    }

    return indexNumbers;
  }

  /**
   * gets the index number according to their field name.
   *
   * @param fieldName the header value to check.
   * @param indexNumbers the map of header values and indexes.
   * @return int of index
   */
  protected static int getIndexNumberForField(Map<String, Integer> indexNumbers, String fieldName) {
    if (indexNumbers.containsKey(fieldName)) {
      return indexNumbers.get(fieldName);
    }

    throw new IllegalStateException(
        "NPI Org File Processing Error: Cannot field fieldname " + fieldName);
  }
}
