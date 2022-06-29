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
   * The name of the classpath resource (for the project's main web application) for the NPI
   * "Orgs" TSV file.
   */
  public static final String NPI_RESOURCE = "npiorgdata.tsv";

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
  public static void main(String[] args) {
    if (args.length < 1) {
      throw new IllegalArgumentException("OUTPUT_DIR argument not specified for NPI download.");
    }
    if (args.length > 1) {
      throw new IllegalArgumentException("Invalid arguments supplied for NPI download.");
    }
    DataUtilityCommons.getNPIOrgNames(args[0], NPI_RESOURCE);
  }
}

