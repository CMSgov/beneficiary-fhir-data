package gov.cms.bfd.data.fda.utility;

import java.io.BufferedOutputStream;
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
import java.util.Comparator;
import java.util.Objects;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Provides an FDA Drug Code file. */
public class DataUtilityCommons {
  private static final Logger LOGGER = LoggerFactory.getLogger(DataUtilityCommons.class);

  /** Size of the buffer to read/write data. */
  private static final int BUFFER_SIZE = 4096;

  /**
   * Gets the fda drug codes from the fda file.
   *
   * @param outputDir the output directory.
   * @param fdaFile the fda file.
   */
  @SuppressWarnings("java:S5443")
  public static void getFDADrugCodes(String outputDir, String fdaFile)
      throws IllegalStateException {
    Path outputPath = Paths.get(outputDir);
    if (!Files.isDirectory(outputPath)) {
      throw new IllegalStateException("OUTPUT_DIR does not exist for FDA NDC download.");
    }

    // Create a temp directory that will be recursively deleted when we're done.
    try {
      Path workingDir = Files.createTempDirectory("fda-data");

      // If the output file isn't already there, go build it.
      Path convertedNdcDataFile = outputPath.resolve(fdaFile);

      try {
        buildProductsResource(convertedNdcDataFile, workingDir);
      } finally {
        // Recursively delete the working dir.
        recursivelyDelete(workingDir);
      }

    } catch (Exception ex) {
      throw new IllegalStateException(ex);
    }
  }

  /**
   * Creates the file in the specified location.
   *
   * @param convertedNdcDataFile the output file/resource to produce.
   * @param workingDir a directory that temporary/working files can be written to.
   * @throws IOException (any errors encountered will be bubbled up).
   */
  public static void buildProductsResource(Path convertedNdcDataFile, Path workingDir)
      throws IOException, IllegalStateException {
    // download FDA NDC file
    Path downloadedNdcZipFile =
        Paths.get(workingDir.resolve("ndctext.zip").toFile().getAbsolutePath());
    if (!Files.isReadable(downloadedNdcZipFile)) {
      ClassLoader classLoader = DataUtilityCommons.class.getClassLoader();
      File ndcFile =
          new File(Objects.requireNonNull(classLoader.getResource("ndctext.zip")).getFile());
      FileUtils.copyFile(ndcFile, new File(downloadedNdcZipFile.toFile().getAbsolutePath()));
    }

    // unzip FDA NDC file
    unzip(downloadedNdcZipFile, workingDir);
    Path originalNdcDataFile = workingDir.resolve("product.txt");
    if (!Files.isReadable(originalNdcDataFile))
      originalNdcDataFile = workingDir.resolve("Product.txt");
    if (!Files.isReadable(originalNdcDataFile))
      throw new IllegalStateException("Unable to locate product.txt in ndctext.zip");

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

    try (FileInputStream is = new FileInputStream(originalNdcDataFile.toFile().getAbsolutePath());
        BufferedReader reader = new BufferedReader(new InputStreamReader(is, inDec));
        FileOutputStream fw =
            new FileOutputStream(convertedNdcDataFile.toFile().getAbsolutePath());
        BufferedWriter out = new BufferedWriter(new OutputStreamWriter(fw, outEnc))) {

      for (String in; (in = reader.readLine()) != null; ) {
        out.write(in);
        out.newLine();
      }
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
  @SuppressWarnings("java:S5042")
  public static void unzip(Path zipFilePath, Path destDirectory) throws IOException {
    try (ZipInputStream zipIn = new ZipInputStream(new FileInputStream(zipFilePath.toFile()))) {
      ZipEntry entry = zipIn.getNextEntry();
      // iterates over entries in the zip file
      while (entry != null) {
        Path filePath = destDirectory.resolve(entry.getName()).normalize();
        if (!filePath.startsWith(destDirectory)) {
          // Checks if resolved path is trying to escape from the destination directory
          throw new IOException("ZipSlip: Entry is outside of the target directory");
        }
        if (!entry.isDirectory()) {
          // if the entry is a file, extracts it
          extractFile(zipIn, filePath);
        } else {
          // if the entry is a directory, make the directory
          Files.createDirectories(filePath);
        }
        zipIn.closeEntry();
        entry = zipIn.getNextEntry();
      }
    }
  }

  /**
   * Extracts a zip entry (file entry).
   *
   * @param zipIn the zip file coming in
   * @param filePath the file path for the file
   * @throws IOException (any errors encountered will be bubbled up)
   */
  public static void extractFile(ZipInputStream zipIn, Path filePath) throws IOException {
    Files.createDirectories(filePath.getParent());
    try (BufferedOutputStream bos =
        new BufferedOutputStream(new FileOutputStream(filePath.toFile().getAbsolutePath()))) {
      byte[] bytesIn = new byte[BUFFER_SIZE];
      int read;
      while ((read = zipIn.read(bytesIn)) != -1) {
        bos.write(bytesIn, 0, read);
      }
    }
  }

  /**
   * Recursively delete temp directory.
   *
   * @param tempDir is the temp directory.
   */
  private static void recursivelyDelete(Path tempDir) {
    // Recursively delete the working dir.
    try (Stream<Path> paths = Files.walk(tempDir)) {
      paths
          .sorted(Comparator.reverseOrder())
          .map(Path::toFile)
          .peek(System.out::println)
          .forEach(
              file -> {
                if (!file.delete()) {
                  LOGGER.warn("Failed to delete file: " + file);
                }
              });
    } catch (IOException e) {
      LOGGER.warn("Failed to cleanup the temporary folder", e);
    }
  }
}
