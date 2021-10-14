package gov.cms.bfd.pipeline.bridge.etl;

import gov.cms.bfd.pipeline.bridge.io.Source;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class RifParser implements Parser<String> {

  private static final String DELIMITER = "\\|";

  private final Source<String> source;
  private final Map<String, Integer> headerIndexMap = new HashMap<>();

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
  public static class RifData implements Data<String> {

    private final Map<String, Integer> headerIndexMap;
    private final String[] rowData;

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
  }
}
