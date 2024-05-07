package gov.cms.model.dsl.codegen.plugin.mappers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import gov.cms.model.dsl.codegen.plugin.model.FhirElementBean;
import java.io.Closeable;
import java.io.IOException;
import java.io.Writer;
import java.util.function.Function;

/** Processor that takes a stream of FhirElements and writes them to a single JSON file. */
public class FhirElementToJson implements Function<FhirElementBean, FhirElementBean>, Closeable {

  /** JSON array element separator. */
  private static final String SEPARATOR = ",";

  /** Opening bracket for JSON array. */
  private static final String ARRAY_OPEN = "[";

  /** Closing bracket and newline for JSON array. */
  private static final String ARRAY_CLOSE = "]\n";

  /** Object mapper for serializing/deserializing JSON. */
  private static final ObjectMapper objectMapper =
      com.fasterxml.jackson.databind.json.JsonMapper.builder()
          .configure(SerializationFeature.INDENT_OUTPUT, true)
          .build();

  /** Writer used to persist JSON. */
  private final Writer writer;

  /** Flag indicating that the first element has been written. */
  private boolean started;

  /**
   * Creates an instance of a FhirElementToJson given a Writer.
   *
   * @param writer the writer to use to persist the JSON
   * @return a FhirElementToJson
   * @throws IOException upon write errors
   */
  public static FhirElementToJson createInstance(Writer writer) throws IOException {
    var FhirElementToJson = new FhirElementToJson(writer);
    FhirElementToJson.init();
    return FhirElementToJson;
  }

  /**
   * Serializes a FhirElement to JSON and writes it.
   *
   * @param element the FhirElement to write
   * @return a FhirElement
   */
  @Override
  public FhirElementBean apply(FhirElementBean element) {
    try {
      if (started) {
        writer.write(SEPARATOR);
      } else {
        started = true;
      }
      writer.write(objectMapper.writeValueAsString(element));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return element;
  }

  /**
   * Closes the writer.
   *
   * @throws IOException upon write or closing errors
   */
  @Override
  public void close() throws IOException {
    writer.write(ARRAY_CLOSE);
    writer.close();
  }

  /**
   * Private constructor.
   *
   * @param writer the writer to use to persist the JSON.
   */
  private FhirElementToJson(Writer writer) {
    this.writer = writer;
  }

  /**
   * Initializes the FhirElementToJson.
   *
   * @throws IOException upon write errors
   */
  private void init() throws IOException {
    writer.write(ARRAY_OPEN);
  }
}
