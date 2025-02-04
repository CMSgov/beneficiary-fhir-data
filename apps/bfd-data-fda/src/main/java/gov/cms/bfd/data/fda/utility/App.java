package gov.cms.bfd.data.fda.utility;

/** Provides an FDA Drug Code file to download. */
public class App {
  /**
   * The name of the classpath resource (for the project's main web application) for the FDA
   * "Products" TSV file.
   */
  public static final String FDA_PRODUCTS_RESOURCE = "fda_products_utf8.tsv";

  /**
   * The application entry point, which will receive all non-JVM command line options in the <code>
   * args</code> array.
   *
   * @param args
   *     <p>The non-JVM command line arguments that the application was launched with. Must include:
   *     <ol>
   *       <li><code>OUTPUT_DIR</code>: the first (and only) argument for this application, which
   *           should be the path to the project's resource directory
   *     </ol>
   *
   * @throws IllegalArgumentException if there is an issue creating or iterating over the downloaded
   *     files
   */
  public static void main(String[] args) {
    if (args.length < 2) {
      throw new IllegalArgumentException(
          "Invalid number of arguments supplied for FDA NDC download.");
    }
    if (args.length > 2) {
      throw new IllegalArgumentException("Invalid arguments supplied for FDA NDC download.");
    }

    DataUtilityCommons.getFDADrugCodes(args[0], args[1], FDA_PRODUCTS_RESOURCE);
  }
}
