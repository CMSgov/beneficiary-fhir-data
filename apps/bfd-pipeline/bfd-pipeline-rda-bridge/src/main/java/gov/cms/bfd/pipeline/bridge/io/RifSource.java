package gov.cms.bfd.pipeline.bridge.io;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Creates a {@link Source} that reads data from a RIF file. */
public class RifSource implements Source<String> {

  /** Buffered Reader for inputPath. */
  private final BufferedReader reader;
  /** Next Line to keep track of reader. */
  private String nextLine;

  /**
   * RifSource constructor method.
   *
   * @param inputPath is the inputpath
   * @throws IOException throws IOException
   */
  public RifSource(Path inputPath) throws IOException {
    reader = Files.newBufferedReader(inputPath, StandardCharsets.UTF_8);
    nextLine = reader.readLine();
  }

  /** {@inheritDoc} */
  @Override
  public boolean hasInput() {
    return nextLine != null;
  }

  /** {@inheritDoc} */
  @Override
  public String read() throws IOException {
    if (nextLine == null) {
      throw new IOException("End of source reached.");
    }

    String line = nextLine;
    nextLine = reader.readLine();
    return line;
  }

  /** {@inheritDoc} */
  @Override
  public void close() throws IOException {
    reader.close();
  }
}
