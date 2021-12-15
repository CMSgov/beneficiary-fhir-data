package gov.cms.bfd.pipeline.bridge.io;

import java.io.Closeable;
import java.io.IOException;

public interface Source<T> extends Closeable {

  boolean hasInput();

  T read() throws IOException;
}
