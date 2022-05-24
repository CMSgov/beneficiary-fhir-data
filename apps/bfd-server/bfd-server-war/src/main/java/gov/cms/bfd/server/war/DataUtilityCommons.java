package gov.cms.bfd.server.war;

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

/** Commons library for upzipping files */
public final class DataUtilityCommons {
  private static final Logger LOGGER = LoggerFactory.getLogger(DataUtilityCommons.class);

  /** Size of the buffer to read/write data */
  private static final int BUFFER_SIZE = 4096;

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

    if (matchingFiles.length > 1) {
      throw new IllegalStateException("More than one NPI file found");
    }

    Path originalNpiDataFile = workingDir.resolve(matchingFiles[0].getName());
    if (!Files.isReadable(originalNpiDataFile))
      throw new IllegalStateException("Unable to locate npidata_pfile in " + ndctextZipUrl);
    return originalNpiDataFile;
  }

  /**
   * Creates the file in the specified location.
   *
   * @param convertedNdcDataFile the output file/resource to produce
   * @param workingDir a directory that temporary/working files can be written to
   * @throws IOException (any errors encountered will be bubbled up)
   */
  public static void buildProductsResource(Path convertedNdcDataFile, Path workingDir)
      throws IOException {
    // download FDA NDC file
    Path downloadedNdcZipFile =
        Paths.get(workingDir.resolve("ndctext.zip").toFile().getAbsolutePath());
    URL ndctextZipUrl = new URL("https://www.accessdata.fda.gov/cder/ndctext.zip");
    if (!Files.isReadable(downloadedNdcZipFile)) {
      // connectionTimeout, readTimeout = 10 seconds
      FileUtils.copyURLToFile(
          ndctextZipUrl, new File(downloadedNdcZipFile.toFile().getAbsolutePath()), 10000, 10000);
    }

    // unzip FDA NDC file
    unzip(downloadedNdcZipFile, workingDir);
    Path originalNdcDataFile = workingDir.resolve("product.txt");
    if (!Files.isReadable(originalNdcDataFile))
      originalNdcDataFile = workingDir.resolve("Product.txt");
    if (!Files.isReadable(originalNdcDataFile))
      throw new IllegalStateException("Unable to locate product.txt in " + ndctextZipUrl);

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
}
