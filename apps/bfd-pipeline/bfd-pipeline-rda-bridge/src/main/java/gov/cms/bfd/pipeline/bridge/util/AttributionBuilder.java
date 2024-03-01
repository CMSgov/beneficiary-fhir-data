package gov.cms.bfd.pipeline.bridge.util;

import com.google.common.collect.Lists;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Helper class for building the attribution sql script.
 *
 * <p>See <a href="https://freemarker.apache.org/docs/">FreeMarker</a> for templating information
 */
@Slf4j
@RequiredArgsConstructor
public class AttributionBuilder {

  /** Attribution template is the file path of the file. */
  private final String attributionTemplate;

  /** Attribution Script is the file name for the script. */
  private final String attributionScript;

  /**
   * Runs the attribution builder logic, reading the given template file and producing a new script
   * in the given file location, interpolated with the given {@link DataSampler} dataset.
   *
   * @param dataSampler The {@link DataSampler} set to pull data from.
   */
  public void run(DataSampler<String> dataSampler) {
    try (BufferedWriter writer = new BufferedWriter(new FileWriter(attributionScript))) {
      Path templatePath = Path.of(attributionTemplate);
      Path baseDir = templatePath.getParent();
      String templateFile = templatePath.getFileName().toString();
      Configuration config = new Configuration(Configuration.VERSION_2_3_31);
      config.setDirectoryForTemplateLoading(baseDir.toFile());
      config.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
      config.setLogTemplateExceptions(false);

      Template t = config.getTemplate(templateFile);
      List<String> values = Lists.newArrayList(dataSampler);
      t.process(Map.of("value", values), writer);
    } catch (IOException | TemplateException e) {
      log.error("Unable to create attribution sql script", e);
    }
  }
}
