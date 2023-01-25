package gov.cms.bfd.pipeline.bridge.io;

import java.io.Closeable;
import java.io.IOException;

/** Interface for Source that extends Closeable. */
public interface Source<T> extends Closeable {

  /**
   * hasInput() method returns {@link boolean}.
   *
   * @return {@link boolean}.
   */
  boolean hasInput();

  /**
   * Interface generic that returns {@link T}.
   *
   * @return {@link T}.
   */
  T read() throws IOException;
}
