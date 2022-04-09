package gov.cms.model.dsl.codegen.plugin;

import java.io.File;
import org.apache.maven.plugin.MojoExecutionException;

public class MojoUtil {

  public static File initializeOutputDirectory(String outputDirectory) {
    File outputDir = new File(outputDirectory);
    outputDir.mkdirs();
    return outputDir;
  }

  public static MojoExecutionException createException(String formatString, Object... args) {
    String message = String.format(formatString, args);
    return new MojoExecutionException(message);
  }
}
