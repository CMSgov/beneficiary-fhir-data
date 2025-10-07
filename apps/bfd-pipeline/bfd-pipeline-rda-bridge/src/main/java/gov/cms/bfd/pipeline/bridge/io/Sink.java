package gov.cms.bfd.pipeline.bridge.io;

import java.io.Closeable;

/**
 * Interface for the Sink that extends Closeable.
 *
 * @param <T> the type of items accepted by this Sink
 */
public interface Sink<T> extends Closeable {
  /**
   * Writes a value to the sink (for example, to a file or stream).
   *
   * @param value the value to be written
   */
  void write(T value);
}
