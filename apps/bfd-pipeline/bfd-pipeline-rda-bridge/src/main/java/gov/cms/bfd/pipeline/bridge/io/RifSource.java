package gov.cms.bfd.pipeline.bridge.io;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Creates a {@link Source} that reads data from a RIF file. */
public class RifSource implements Source<String> {

  private final BufferedReader reader;

  private String nextLine;

  public RifSource(Path inputPath) throws IOException {
    reader = Files.newBufferedReader(inputPath, StandardCharsets.UTF_8);
    nextLine = reader.readLine();
  }

  @Override
  public boolean hasInput() {
    return nextLine != null;
  }

  @Override
  public String read() throws IOException {
    if (nextLine == null) {
      throw new IOException("End of source reached.");
    }

    String line = nextLine;
    nextLine = reader.readLine();
    return line;
  }

  @Override
  public void close() throws IOException {
    reader.close();
  }
}
