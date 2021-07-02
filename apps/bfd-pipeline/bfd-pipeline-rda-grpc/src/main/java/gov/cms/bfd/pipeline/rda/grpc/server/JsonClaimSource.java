package gov.cms.bfd.pipeline.rda.grpc.server;

import com.google.protobuf.util.JsonFormat;
import gov.cms.mpsm.rda.v1.fiss.FissClaim;
import gov.cms.mpsm.rda.v1.mcs.McsClaim;
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
public class JsonClaimSource<T> implements ClaimSource<T> {
  private final Parser<T> parser;
  private final BufferedReader reader;
  private String line;

  /**
   * Produce a JsonFissClaimSource that parses the provided JSONL data.
   *
   * @param json String containing one or more lines of JSONL data
   */
  public JsonClaimSource(String json, Parser<T> parser) {
    reader = new BufferedReader(new StringReader(json));
    this.parser = parser;
  }

  /**
   * Produce a JsonFissClaimSource that parses all of the provided JSONL data. Each value in the
   * list can contain one or more lines of JSONL data.
   *
   * @param lines List of JSONL data to be concatenated and parsed.
   */
  public JsonClaimSource(List<String> lines, Parser<T> parser) {
    this(String.join(System.lineSeparator(), lines), parser);
  }

  /**
   * Produce a JsonFissClaimSource that parses the JSONL contents of the specified File.
   *
   * @param filename identifies a JSONL file containing message objects
   */
  public JsonClaimSource(File filename, Parser<T> parser) {
    try {
      reader = new BufferedReader(new FileReader(filename));
    } catch (FileNotFoundException e) {
      throw new RuntimeException(e);
    }
    this.parser = parser;
  }

  /**
   * This method fits the signature of Parser&lt;FissClaim&gt;
   *
   * @param jsonString JSON to be parsed
   * @return a FissClaim object
   * @throws Exception any error caused by invalid JSON
   */
  public static FissClaim parseFissClaim(String jsonString) throws Exception {
    FissClaim.Builder claim = FissClaim.newBuilder();
    JsonFormat.parser().merge(jsonString, claim);
    return claim.build();
  }

  /**
   * This method fits the signature of Parser&lt;McsClaim&gt;
   *
   * @param jsonString JSON to be parsed
   * @return a FissClaim object
   * @throws Exception any error caused by invalid JSON
   */
  public static McsClaim parseMcsClaim(String jsonString) throws Exception {
    McsClaim.Builder claim = McsClaim.newBuilder();
    JsonFormat.parser().merge(jsonString, claim);
    return claim.build();
  }

  @Override
  public boolean hasNext() throws Exception {
    return advance();
  }

  @Override
  public T next() throws Exception {
    if (!advance()) {
      throw new NoSuchElementException();
    }
    final T claim = parser.parseJson(line);
    line = null;
    return claim;
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

  /**
   * The Protobuf Message.Builder can throw a checked exception so we need an interface that allows
   * that for use in our lambda expression. This is only expected to be implemented here in a static
   * method so it can be passed to our constructor.
   *
   * @param <T>
   */
  @FunctionalInterface
  public interface Parser<T> {
    T parseJson(String jsonString) throws Exception;
  }
}
