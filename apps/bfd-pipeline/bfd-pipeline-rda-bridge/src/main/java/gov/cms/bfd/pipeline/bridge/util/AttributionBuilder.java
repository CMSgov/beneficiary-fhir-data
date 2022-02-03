package gov.cms.bfd.pipeline.bridge.util;

import com.github.jknack.handlebars.Context;
import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Helper;
import com.github.jknack.handlebars.Template;
import com.github.jknack.handlebars.io.FileTemplateLoader;
import com.github.jknack.handlebars.io.TemplateLoader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Helper class for building the attribution sql script.
 *
 * <p>The design utilizes templates to build custom sql scripts based on user configuration.
 *
 * <h3>Example script</h3>
 *
 * <p>insert into my_table ("some_value") values {{#SQLValues
 * this}}('{{this}}'){{#if @last}};{{else}},{{/if}} {{/SQLValues}}
 *
 * <p>This would print
 *
 * <p>insert into my_table ("some_value") values ('value1'), ('value2'), ('value3'), --....
 * ('valueN');
 *
 * <p>A sublist can also be denoted
 *
 * <p>insert into my_table ("some_value") values {{#SQLValues this
 * 2}}('{{this}}'){{#if @last}};{{else}},{{/if}} {{/SQLValues}}
 *
 * <p>This would print
 *
 * <p>insert into my_table ("some_value") values ('value1'), ('value2');
 */
@Slf4j
@RequiredArgsConstructor
public class AttributionBuilder {

  private final String attributionTemplate;
  private final String attributionScript;

  /**
   * Runs the attribution builder logic, reading the given template file and producing a new script
   * in the given file location, interpolated with the given {@link DataSampler} dataset.
   *
   * @param dataSampler The {@link DataSampler} set to pull data from.
   */
  public void run(DataSampler<String> dataSampler) {
    // Handlebars needs to be told if this is a relative or absolute path
    String baseDir = attributionTemplate.charAt(0) == '/' ? "/" : ".";

    TemplateLoader loader = new FileTemplateLoader(baseDir, "");
    Handlebars handlebars = new Handlebars(loader);

    // This will apply the helper logic to any {{#SQLValues}} block.
    handlebars.registerHelper("SQLValues", sqlValuesHelper());

    try (BufferedWriter writer = new BufferedWriter(new FileWriter(attributionScript))) {
      Template template = handlebars.compile(attributionTemplate);
      // Template#apply() seems to parse the entire thing into a single string, not
      // sure if this will cause issues with larger data sets.
      writer.write(template.apply(dataSampler));
    } catch (IOException e) {
      log.error("Unable to create attribution sql script", e);
    }
  }

  /**
   * Helper for the Handlebars templating library. This defines how to process a particular template
   * "block", as well as defining how to treat the data for it. In this case, we're saying it's an
   * {@link Iterable<String>}.
   *
   * @return The helper to be used by the templating library.
   */
  private Helper<Iterable<String>> sqlValuesHelper() {
    return (objects, options) -> {
      int subListSize = options.param(0, 0);

      if (subListSize < 0) {
        subListSize = 0;
      }

      StringBuilder sb = new StringBuilder();

      Iterator<String> iterator = objects.iterator();

      int i = 0;
      while (iterator.hasNext() && i < subListSize) {
        String value = iterator.next();

        // If this is the last entry in the iterator, we need to set the @last flag to true
        // which is used in the template.  If other flags (such as @first) are desired,
        // additional logic would need to be added for those.
        if (iterator.hasNext() && i < (subListSize - 1)) {
          sb.append(options.fn(value));
        } else {
          sb.append(options.fn(Context.newBuilder(value).combine("last", true).build()));
        }

        ++i;
      }

      return sb.toString();
    };
  }
}
