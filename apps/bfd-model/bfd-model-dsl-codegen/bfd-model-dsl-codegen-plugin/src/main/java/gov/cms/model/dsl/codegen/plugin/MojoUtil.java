package gov.cms.model.dsl.codegen.plugin;

import java.io.File;
import org.apache.maven.plugin.MojoExecutionException;

/** Utility methods for use in all {@link org.apache.maven.plugin.Mojo} implementations. */
public class MojoUtil {

  /** Prevents instantiation of static utility class. */
  private MojoUtil() {}

  /**
   * Ensures that the specified directory and its parent directories exist.
   *
   * @param outputDirectory path to a directory to be created/verified
   * @return {@link File} representing to the directory
   */
  public static File initializeOutputDirectory(String outputDirectory) {
    File outputDir = new File(outputDirectory);
    outputDir.mkdirs();
    return outputDir;
  }

  /**
   * Creates an exception of {@link MojoExecutionException} class with an error message built by
   * applying the specified {@link String#format} format string and arguments.
   *
   * @param formatString {@link String#format} compatible format string
   * @param args any values required to populate arguments in the format string
   * @return an exception with the specified message
   */
  public static MojoExecutionException createException(String formatString, Object... args) {
    String message = String.format(formatString, args);
    return new MojoExecutionException(message);
  }
}
