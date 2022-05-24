package gov.cms.bfd.server.war;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;

/**
 * A simple application that downloads the FDA NDC (national drug code) file; unzips it and then
 * converts it to UTF-8 format.
 *
 * <p>See the <code>download-fda-drug-data</code> execution of <code>exec-maven-plugin</code> in
 * this project's <code>pom.xml</code> for details on how this utility is run during the project's
 * build.
 */
public final class FDADrugDataUtilityApp {
  /**
   * The name of the classpath resource (for the project's main web application) for the FDA
   * "Products" TSV file.
   */
  public static final String FDA_PRODUCTS_RESOURCE = "fda_products_cp1252.tsv";

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
      System.err.println("OUTPUT_DIR argument not specified for FDA NDC download.");
      System.exit(1);
    }
    if (args.length > 1) {
      System.err.println("Invalid arguments supplied for FDA NDC download.");
      System.exit(2);
    }

    Path outputPath = Paths.get(args[0]);
    if (!Files.isDirectory(outputPath)) {
      System.err.println("OUTPUT_DIR does not exist for FDA NDC download.");
      System.exit(3);
    }

    // Create a temp directory that will be recursively deleted when we're done.
    Path workingDir = Files.createTempDirectory("fda-data");

    // If the output file isn't already there, go build it.
    Path convertedNdcDataFile = outputPath.resolve(FDA_PRODUCTS_RESOURCE);
    if (!Files.exists(convertedNdcDataFile)) {
      try {
        DataUtilityCommons.buildProductsResource(convertedNdcDataFile, workingDir);
      } finally {
        // Recursively delete the working dir.
        Files.walk(workingDir)
            .sorted(Comparator.reverseOrder())
            .map(Path::toFile)
            .peek(System.out::println)
            .forEach(File::delete);
      }
    }
  }
}
