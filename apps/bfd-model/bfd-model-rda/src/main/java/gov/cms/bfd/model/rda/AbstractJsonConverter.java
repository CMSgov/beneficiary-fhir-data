package gov.cms.bfd.model.rda;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import javax.annotation.Nullable;
import javax.persistence.AttributeConverter;

/**
 * Base class for {@code AttributeConverter} instances that provide a common mechanism for
 * converting our POJOs into JSON for inclusion in a JSONB postgresql column or varchar HSQLDB
 * column. One concrete subclass would be defined for each root object class that needs to be
 * converted to JSON.
 *
 * @param <T> type of one of our data POJOs
 */
public class AbstractJsonConverter<T> implements AttributeConverter<T, String> {
  /**
   * {@code ObjectMapper} instances are thread safe so this singleton instance ensures consistent
   * formatting behavior for all instances.
   */
  private static final ObjectMapper objectMapper =
      new ObjectMapper()
          .enable(SerializationFeature.INDENT_OUTPUT)
          .registerModule(new Jdk8Module())
          .registerModule(new JavaTimeModule())
          .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
          .setSerializationInclusion(JsonInclude.Include.NON_NULL);

  private final Class<T> klass;

  protected AbstractJsonConverter(Class<T> klass) {
    this.klass = klass;
  }

  @Override
  public String convertToDatabaseColumn(T attribute) {
    return objectToJson(attribute);
  }

  @Override
  public T convertToEntityAttribute(String dbData) {
    return jsonToObject(dbData);
  }

  @Nullable
  private String objectToJson(@Nullable Object value) {
    try {
      if (value == null) {
        return null;
      }
      return objectMapper.writeValueAsString(value);
    } catch (final Exception ex) {
      throw new RuntimeException(
          String.format("Failed to convert %s to JSON: %s", klass.getSimpleName(), ex.getMessage()),
          ex);
    }
  }

  @Nullable
  private T jsonToObject(@Nullable String value) {
    try {
      if (value == null) {
        return null;
      }
      return objectMapper.readValue(value, klass);
    } catch (final Exception ex) {
      throw new RuntimeException(
          String.format("Failed to convert JSON to %s: %s", klass.getSimpleName(), ex.getMessage()),
          ex);
    }
  }
}
