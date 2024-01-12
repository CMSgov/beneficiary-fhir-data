package gov.cms.bfd.sharedutils.json;

/** Interface for objects that can convert JSON strings into java beans and vice versa. */
public interface JsonConverter {
  /**
   * Convert the provided JSON string into an object of the specified class.
   *
   * @param jsonString json representation of the object
   * @param objectClass the class of the object represented by the json string
   * @return the converted object
   * @param <T> type of object to convert
   * @throws JsonConversionException if conversion fails
   */
  <T> T jsonToObject(String jsonString, Class<T> objectClass) throws JsonConversionException;

  /**
   * Convert the provided object into a JSON string representation.
   *
   * @param object object to convert
   * @return JSON representation of the object
   * @param <T> type of object to convert
   * @throws JsonConversionException if conversion fails
   */
  <T> String objectToJson(T object) throws JsonConversionException;

  /**
   * Return an instance that produces pretty printed JSON.
   *
   * @return the instance
   */
  static JsonConverter prettyInstance() {
    return JacksonJsonConverter.prettyInstance();
  }

  /**
   * Return an instance that produces minimal JSON.
   *
   * @return the instance
   */
  static JsonConverter minimalInstance() {
    return JacksonJsonConverter.minimalInstance();
  }
}
