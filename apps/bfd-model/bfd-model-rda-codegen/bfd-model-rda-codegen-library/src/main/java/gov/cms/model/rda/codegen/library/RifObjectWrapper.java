package gov.cms.model.rda.codegen.library;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Objects;
import org.apache.commons.csv.CSVRecord;

public class RifObjectWrapper {
  private final List<CSVRecord> lines;
  private final CSVRecord header;

  public RifObjectWrapper(List<CSVRecord> csvRecords) {
    // Verify the inputs.
    Objects.requireNonNull(csvRecords);
    if (csvRecords.isEmpty()) {
      throw new IllegalArgumentException();
    }
    lines = csvRecords;
    header = lines.get(0);
  }

  public RifObjectWrapper(CSVRecord singleLine) {
    lines = ImmutableList.of(singleLine);
    header = singleLine;
  }

  public int getLinesCount() {
    return lines.size();
  }

  public RifObjectWrapper getLines(int index) {
    return new RifObjectWrapper(lines.get(index));
  }

  /**
   * Tests whether a value exists for {@code label}.
   *
   * @param label the column label for the field to be checked
   * @return true if the value is non-null and non-empty
   */
  public boolean hasValue(final String label) {
    return !Strings.isNullOrEmpty(header.get(label));
  }

  /**
   * Returns a (possibly empty) value for {@code label}.
   *
   * @param label the column label for the field to be checked
   * @return the String at the given enum String
   */
  public String getValue(final String label, String defaultValue) {
    var value = header.get(label);
    if (value != null && value.isEmpty() && defaultValue != null) {
      value = defaultValue;
    }
    return value;
  }
}
