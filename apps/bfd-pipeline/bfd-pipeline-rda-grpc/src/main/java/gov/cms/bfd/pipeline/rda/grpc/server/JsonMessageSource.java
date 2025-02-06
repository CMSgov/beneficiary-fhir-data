package gov.cms.bfd.pipeline.rda.grpc.server;

import com.google.common.collect.ImmutableList;
import com.google.common.io.CharSource;
import com.google.protobuf.util.JsonFormat;
import gov.cms.mpsm.rda.v1.ClaimSequenceNumberRange;
import gov.cms.mpsm.rda.v1.FissClaimChange;
import gov.cms.mpsm.rda.v1.McsClaimChange;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * A {@link MessageSource} implementation that produces objects from NDJSON data. The grpc-java
 * library includes a JsonFormat class that can be used to convert gRPC message objects into JSON
 * strings and vice versa. The NDJSON data must contain one valid message object JSON per line.
 *
 * @param <T> the message type
 */
public class JsonMessageSource<T> implements MessageSource<T> {
  /** The JSON parser. */
  private final Parser<T> parser;

  /** Reads files from the file system. */
  private final BufferedReader reader;

  /** The next message to return. */
  private T nextMessage;

  /**
   * Produce a JsonMessageSource that parses the provided NDJSON data.
   *
   * @param json String containing one or more lines of NDJSON data
   * @param parser the parser to convert a line of JSON into an object
   */
  public JsonMessageSource(String json, Parser<T> parser) {
    reader = new BufferedReader(new StringReader(json));
    this.parser = parser;
  }

  /**
   * Produce a JsonMessageSource that parses all the provided NDJSON data. Each value in the list
   * can contain one or more lines of NDJSON data.
   *
   * @param lines List of NDJSON data to be concatenated and parsed.
   * @param parser the parser to convert a line of JSON into an object
   */
  public JsonMessageSource(List<String> lines, Parser<T> parser) {
    this(String.join(System.lineSeparator(), lines), parser);
  }

  /**
   * Produce a JsonMessageSource that parses the NDJSON contents of the specified File.
   *
   * @param filename identifies a NDJSON file containing message objects
   * @param parser the parser to convert a line of JSON into an object
   */
  public JsonMessageSource(File filename, Parser<T> parser) {
    try {
      reader = new BufferedReader(new FileReader(filename));
    } catch (FileNotFoundException e) {
      throw new RuntimeException(e);
    }
    this.parser = parser;
  }

  /**
   * Produce a JsonMessageSource that parses the NDJSON contents of the specified {@link
   * CharSource}.
   *
   * @param charSource source of a NDJSON file containing message objects
   * @param parser the parser to convert a line of JSON into an object
   */
  public JsonMessageSource(CharSource charSource, Parser<T> parser) {
    try {
      reader = charSource.openBufferedStream();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    this.parser = parser;
  }

  /**
   * Returns a {@link Parser} instance for parsing {@link FissClaimChange} JSON.
   *
   * @return the parser
   */
  public static Parser<FissClaimChange> fissParser() {
    return new Parser<>() {
      @Override
      public FissClaimChange parseJson(String jsonString) throws Exception {
        FissClaimChange.Builder claim = FissClaimChange.newBuilder();
        JsonFormat.parser().merge(jsonString, claim);
        return claim.build();
      }

      @Override
      public long sequenceNumberOf(FissClaimChange message) {
        return message.getSeq();
      }
    };
  }

  /**
   * Returns a {@link Parser} instance for parsing {@link McsClaimChange} JSON.
   *
   * @return the parser
   */
  public static Parser<McsClaimChange> mcsParser() {
    return new Parser<>() {
      @Override
      public McsClaimChange parseJson(String jsonString) throws Exception {
        McsClaimChange.Builder claim = McsClaimChange.newBuilder();
        JsonFormat.parser().merge(jsonString, claim);
        return claim.build();
      }

      @Override
      public long sequenceNumberOf(McsClaimChange message) {
        return message.getSeq();
      }
    };
  }

  /**
   * Parse every json string in the input to produce a new List of parsed objects.
   *
   * @param jsonStrings collection of JSON strings recognizable by the parser
   * @param parser parser able to parse the strings
   * @param <T> type produced by the porser
   * @return an immutable list of parsed strings
   * @throws Exception any error caused by invalid JSON
   */
  public static <T> ImmutableList<T> parseAll(Iterable<String> jsonStrings, Parser<T> parser)
      throws Exception {
    final ImmutableList.Builder<T> builder = ImmutableList.builder();
    for (String jsonString : jsonStrings) {
      builder.add(parser.parseJson(jsonString));
    }
    return builder.build();
  }

  @Override
  public MessageSource<T> skipTo(long startingSequenceNumber) throws Exception {
    while (hasNext() && parser.sequenceNumberOf(nextMessage) < startingSequenceNumber) {
      next();
    }
    return this;
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
    final var answer = nextMessage;
    nextMessage = null;
    return answer;
  }

  @Override
  public ClaimSequenceNumberRange getSequenceNumberRange() {
    // Can't easily support this since it would require deserializing the entire contents
    return ClaimSequenceNumberRange.newBuilder().setLower(0).setUpper(0).build();
  }

  @Override
  public void close() throws Exception {
    reader.close();
  }

  /**
   * Advance the reader and load the next line.
   *
   * @return if there is a next line from the reader
   * @throws IOException there is an issue reading from the reader
   */
  private boolean advance() throws Exception {
    if (nextMessage == null) {
      final var line = reader.readLine();
      if (line != null) {
        nextMessage = parser.parseJson(line);
      }
    }
    return nextMessage != null;
  }

  /**
   * The Protobuf Message.Builder can throw a checked exception so we need an interface that allows
   * that for use in our lambda expression. This is only expected to be implemented here in a static
   * method so it can be passed to our constructor.
   *
   * @param <T> the type parameter
   */
  public interface Parser<T> {
    /**
     * Parses JSON from the specified string.
     *
     * @param jsonString the json string to parse
     * @return the parsed JSON as an object of type T
     * @throws Exception if there is a parse exception
     */
    T parseJson(String jsonString) throws Exception;

    /**
     * Extracts the sequence number from a parsed message.
     *
     * @param message message containing a sequence number
     * @return the sequence number
     */
    long sequenceNumberOf(T message);
  }
}
