package gov.cms.bfd.sharedutils.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;

/** Implementation of {@link JsonConverter} that uses Jackson library for conversion. */
public class JacksonJsonConverter implements JsonConverter {
  /** An instance that produces pretty printed JSON. */
  private static final JacksonJsonConverter PRETTY_PRINTING_INSTANCE =
      new JacksonJsonConverter(true);

  /** An instance that produces minimal JSON. */
  private static final JacksonJsonConverter MINIMAL_INSTANCE = new JacksonJsonConverter(false);

  /** Used to convert java beans into JSON strings. */
  private final ObjectWriter writer;

  /** Used to convert JSON strings into java beans. */
  private final ObjectReader reader;

  /**
   * Return an instance that produces pretty printed JSON.
   *
   * @return the instance
   */
  public static JacksonJsonConverter prettyInstance() {
    return PRETTY_PRINTING_INSTANCE;
  }

  /**
   * Return an instance that produces minimal JSON.
   *
   * @return the instance
   */
  public static JacksonJsonConverter minimalInstance() {
    return MINIMAL_INSTANCE;
  }

  /**
   * Initializes an instance.
   *
   * @param prettyPrint when true JSON strings will be multi-line with indented fields
   */
  public JacksonJsonConverter(boolean prettyPrint) {
    var jsonMapper =
        JsonMapper.builder()
            .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .addModule(new Jdk8Module())
            .addModule(new JavaTimeModule())
            .serializationInclusion(JsonInclude.Include.NON_NULL)
            .build();
    writer = prettyPrint ? jsonMapper.writerWithDefaultPrettyPrinter() : jsonMapper.writer();
    reader = jsonMapper.reader();
  }

  @Override
  public <T> T jsonToObject(String jsonString, Class<T> objectClass)
      throws JsonConversionException {
    try {
      return reader.readValue(jsonString, objectClass);
    } catch (IOException ex) {
      throw new JsonConversionException(ex);
    }
  }

  @Override
  public <T> String objectToJson(T object) throws JsonConversionException {
    try {
      return writer.writeValueAsString(object);
    } catch (IOException ex) {
      throw new JsonConversionException(ex);
    }
  }
}
