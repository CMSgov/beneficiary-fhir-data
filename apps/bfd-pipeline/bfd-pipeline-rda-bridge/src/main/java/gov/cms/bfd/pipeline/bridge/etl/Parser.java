package gov.cms.bfd.pipeline.bridge.etl;

import java.io.Closeable;
import java.io.IOException;
import java.util.Optional;

/**
 * A {@link Parser} is implemented to define how to pull data from a given source.
 *
 * <p>The {@link Parser} creates {@link Data} objects that are then used to retrieve the data in a
 * common manner.
 *
 * @param <T> The type of data that the created {@link Data} objects will return.
 */
public interface Parser<T> extends Closeable {

  /**
   * Defines any initialization steps that need to be done before the parser is used.
   *
   * @throws IOException If there was a file handling error.
   */
  default void init() throws IOException {}

  /**
   * Determines if the {@link Parser} contains any additional data to return.
   *
   * @return True if there is more data to return, false otherwise.
   */
  boolean hasData();

  /**
   * Reads data from the parsed file, returning a {@link Data} object containing the parsed data.
   *
   * @return A {@link Data} object containing the parsed data.
   * @throws IOException If there was a file handling error.
   */
  Data<T> read() throws IOException;

  /**
   * Used in conjunction with {@link Parser} to return parsed data.
   *
   * <p>{@link Data} objects are created by an implemented {@link Parser} that parses a source file.
   *
   * @param <T>
   */
  // S1610 - Abstract classes for predefined methods
  @SuppressWarnings("squid:S1610")
  abstract class Data<T> {

    public enum Type {
      DATE
    }

    /**
     * Get data from the parsed {@link Data} object that is associated with the gtiven fieldName.
     *
     * @param fieldName The name associated w"ith the data being retrieved.
     * @return An {@link Optional} possibly containing the data that was associated with the given
     *     field name.
     */
    public abstract Optional<T> get(String fieldName);

    /**
     * This allows for custom transformation of data prior to returning the value so that it can be
     * standardized if needed.
     *
     * <p>One example is RIF dates are dd-MMM-yyyy format, so we can convert them to more standard
     * yyyy-mm-dd format in a RifParser specific override of this function.
     *
     * @param fieldName The field name of the data being retried.
     * @param type The expected type of the data being retrieved.
     * @return The default implementation returns unchanged data wrapped in an {@link Optional} that
     *     is associated with the given fieldName.
     */
    // S1172 - Intended to be used by overloading child
    @SuppressWarnings("squid:S1172")
    public Optional<T> getFromType(String fieldName, Type type) {
      return get(fieldName);
    }
  }
}
