package gov.cms.bfd.data.npi.utility;

import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A simple application that downloads NPI Data file; unzips it and then converts it to UTF-8
 * format.
 */
public final class App {
  private static final Logger LOGGER = LoggerFactory.getLogger(App.class);
  /**
   * The name of the classpath resource (for the project's main web application) for the NPI "Orgs"
   * TSV file.
   */
  public static final String NPI_RESOURCE = "npi_org_data_utf8.tsv";

  /**
   * The application entry point, which will receive all non-JVM command line options in the <code>
   * args</code> array.
   *
   * @param args
   *     <p>The non-JVM command line arguments that the application was launched with. Must include:
   *     <ol>
   *       <li><code>OUTPUT_DIR</code>: the first (and only) argument for this application, which
   *           should be the path to the project's rsource directory
   *     </ol>
   */
  public static void main(String[] args) throws IOException {
    if (args.length < 1) {
      throw new IllegalArgumentException("OUTPUT_DIR argument not specified for NPI download.");
    }
    if (args.length > 1) {
      throw new IllegalArgumentException("Invalid arguments supplied for NPI download.");
    }
    DataUtilityCommons.getNPIOrgNames(args[0], NPI_RESOURCE);
  }
}
