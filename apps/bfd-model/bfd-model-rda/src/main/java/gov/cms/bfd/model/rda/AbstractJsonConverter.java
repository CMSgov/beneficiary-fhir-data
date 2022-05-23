package gov.cms.bfd.model.rda;

import static java.lang.String.format;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.util.function.Supplier;
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
          .disable(SerializationFeature.INDENT_OUTPUT)
          .registerModule(new Jdk8Module())
          .registerModule(new JavaTimeModule())
          .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
          .setSerializationInclusion(JsonInclude.Include.NON_NULL);

  /** The class of objects being converted to and from JSON. */
  private final Class<T> klass;
  /** Function used to create new instances if the value in the database is null. */
  private final Supplier<T> defaultInstanceFactory;

  /**
   * Produces an instance for the given class that returns {@code null} when deserializing {@code
   * null} values from the database.
   *
   * @param klass Concrete class of objects serialized/deserialized by this converter
   */
  protected AbstractJsonConverter(Class<T> klass) {
    this(klass, () -> null);
  }

  /**
   * Produces an instance for the given class that uses the provided {@link Supplier} to produce
   * default instances when deserializing {@code null} values from the database.
   *
   * @param klass Concrete class of objects serialized/deserialized by this converter
   * @param defaultInstanceFactory {@link Supplier} invoked to create an instance when the JSON
   *     value is null
   */
  protected AbstractJsonConverter(Class<T> klass, Supplier<T> defaultInstanceFactory) {
    this.klass = klass;
    this.defaultInstanceFactory = defaultInstanceFactory;
  }

  /**
   * Convert an object into a JSON String for storage in the database. If the object is null a null
   * value will be returned.
   *
   * @param attribute the entity attribute value to be converted
   * @return the String or null of the entity was null
   */
  @Override
  public String convertToDatabaseColumn(T attribute) {
    try {
      if (attribute == null) {
        return null;
      }
      return objectMapper.writeValueAsString(attribute);
    } catch (final Exception ex) {
      throw new RuntimeException(
          format("Failed to convert %s to JSON: %s", klass.getSimpleName(), ex.getMessage()), ex);
    }
  }

  /**
   * Convert a JSON string into an object. If the database value was null the {@code
   * defaultInstanceFactory} will be used to produce an object.
   *
   * @param dbData the data from the database column to be converted
   * @return an object represented by the string or one created by the {@code
   *     defaultInstanceFactory} if the value is null
   */
  @Override
  public T convertToEntityAttribute(String dbData) {
    try {
      if (dbData == null) {
        return defaultInstanceFactory.get();
      }
      return objectMapper.readValue(dbData, klass);
    } catch (final Exception ex) {
      throw new RuntimeException(
          format("Failed to convert JSON to %s: %s", klass.getSimpleName(), ex.getMessage()), ex);
    }
  }
}
