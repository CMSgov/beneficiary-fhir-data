package gov.cms.bfd.pipeline.rda.grpc.server;

import com.google.protobuf.util.JsonFormat;
import gov.cms.mpsm.rda.v1.fiss.FissClaim;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * A FissClaimSource implementation that produces FissClaim objects from JSONL data. The grpc-java
 * library includes a JsonFormat class that can be used to convert gRPC message objects into JSON
 * strings and vice versa. The JSONL data must contain one valid message object JSON per line.
 */
public class JsonFissClaimSource implements FissClaimSource {
  private final BufferedReader reader;
  private String line;

  /**
   * Produce a JsonFissClaimSource that parses the provided JSONL data.
   *
   * @param json String containing one or more lines of JSONL data
   */
  public JsonFissClaimSource(String json) {
    reader = new BufferedReader(new StringReader(json));
  }

  /**
   * Produce a JsonFissClaimSource that parses all of the provided JSONL data. Each value in the
   * list can contain one or more lines of JSONL data.
   *
   * @param lines List of JSONL data to be concatenated and parsed.
   */
  public JsonFissClaimSource(List<String> lines) {
    this(String.join(System.lineSeparator(), lines));
  }

  /**
   * Produce a JsonFissClaimSource that parses the JSONL contents of the specified File.
   *
   * @param filename identifies a JSONL file containing message objects
   */
  public JsonFissClaimSource(File filename) {
    try {
      reader = new BufferedReader(new FileReader(filename));
    } catch (FileNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public boolean hasNext() throws Exception {
    return advance();
  }

  @Override
  public FissClaim next() throws Exception {
    if (!advance()) {
      throw new NoSuchElementException();
    }
    FissClaim.Builder claim = FissClaim.newBuilder();
    JsonFormat.parser().merge(line, claim);
    line = null;
    return claim.build();
  }

  @Override
  public void close() throws Exception {
    reader.close();
  }

  private boolean advance() throws IOException {
    if (line == null) {
      line = reader.readLine();
    }
    return line != null;
  }
}
