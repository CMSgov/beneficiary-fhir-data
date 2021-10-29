package gov.cms.bfd.pipeline.bridge.etl;

import gov.cms.bfd.pipeline.bridge.io.Source;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class RifParser implements Parser<String> {

  private static final SimpleDateFormat rifDateFormat = new SimpleDateFormat("dd-MMM-yyyy");
  private static final SimpleDateFormat standardFormat = new SimpleDateFormat("yyyy-MM-dd");

  private static final String DELIMITER = "\\|";

  private final Source<String> source;
  private final Map<String, Integer> headerIndexMap = new HashMap<>();

  /**
   * Grabs the headers so they can be used to build maps for the {@link RifData} objects created
   * later.
   *
   * @throws IOException If there was an error handling the file.
   */
  @Override
  public void init() throws IOException {
    if (source.hasInput()) {
      String headerLine = source.read();
      String[] headers = headerLine.split(DELIMITER);

      if (headers.length == 0) {
        throw new IOException("No headers were read");
      }

      for (int i = 0; i < headers.length; ++i) {
        headerIndexMap.put(headers[i], i);
      }
    } else {
      throw new IOException("File was empty, nothing to read");
    }
  }

  @Override
  public boolean hasData() {
    return source.hasInput();
  }

  @Override
  public Data<String> read() throws IOException {
    if (!headerIndexMap.isEmpty()) {
      if (source.hasInput()) {
        return new RifData(headerIndexMap, source.read().split(DELIMITER, 0));
      } else {
        throw new IOException("No mo data to read.");
      }
    }

    throw new IllegalStateException("Parser was not initialized");
  }

  @Override
  public void close() throws IOException {
    source.close();
  }

  @RequiredArgsConstructor
  public static class RifData extends Data<String> {

    private final Map<String, Integer> headerIndexMap;
    private final String[] rowData;

    @Override
    public Optional<String> get(String rifIdentifier) {
      Optional<String> optional;

      // If the cell data is empty, return an empty Optional instead of an empty String.
      if (headerIndexMap.containsKey(rifIdentifier)
          && !rowData[headerIndexMap.get(rifIdentifier)].isEmpty()) {
        optional = Optional.of(rowData[headerIndexMap.get(rifIdentifier)]);
      } else {
        optional = Optional.empty();
      }

      return optional;
    }

    /**
     * Defining a custom {@link Parser.Data#getFromType(String, Type)} here so we can convert RIF
     * style dates to more standard yyyy-mm-dd format.
     *
     * @param rifIdentifier The rif data being retrieved.
     * @param type The expected type of the data being retrieved.
     * @return An {@link Optional} possibly containing the transformed data associated with the
     *     given name.
     */
    @Override
    public Optional<String> getFromType(String rifIdentifier, Data.Type type) {
      return get(rifIdentifier)
          .map(
              value -> {
                if (Type.DATE.equals(type)) {
                  try {
                    return standardFormat.format(rifDateFormat.parse(value));
                  } catch (ParseException e) {
                    throw new IllegalArgumentException("Invalid date format", e);
                  }
                }

                return value;
              });
    }
  }
}
