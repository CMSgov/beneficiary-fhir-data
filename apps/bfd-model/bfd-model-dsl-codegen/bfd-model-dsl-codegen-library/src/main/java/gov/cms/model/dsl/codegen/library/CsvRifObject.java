package gov.cms.model.dsl.codegen.library;

import java.util.Iterator;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import lombok.AllArgsConstructor;
import org.apache.commons.csv.CSVRecord;

@AllArgsConstructor
public class CsvRifObject implements RifObject {
  private final CSVRecord csvRecord;

  @Nullable
  @Override
  public String get(String key) {
    return csvRecord.get(key);
  }

  @Override
  public long getRecordNumber() {
    return csvRecord.getRecordNumber();
  }

  @Override
  public Stream<String> stream() {
    return csvRecord.stream();
  }

  @Nonnull
  @Override
  public Iterator<String> iterator() {
    return csvRecord.iterator();
  }
}
