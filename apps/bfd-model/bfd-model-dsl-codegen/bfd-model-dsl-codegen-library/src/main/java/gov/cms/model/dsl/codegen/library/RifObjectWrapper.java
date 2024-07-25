package gov.cms.model.dsl.codegen.library;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Objects;
import org.apache.commons.csv.CSVRecord;

/**
 * Wrapper around RIF data contained in {@link CSVRecord} objects. Generated code calls these
 * methods to access individual RIF fields by name or index.
 */
public class RifObjectWrapper {
  /**
   * The first record contains the data for the claim object and any additional lines contain data
   * for claim lines.
   */
  private final List<CSVRecord> lines;

  /**
   * Contains the CSV header for the object. Used for determining whether or not a given column
   * label is valid.
   */
  private final CSVRecord header;

  /**
   * Constructs a new object for the given {@link CSVRecord}s.
   *
   * @param csvRecords one or more records containing data for the object
   */
  public RifObjectWrapper(List<CSVRecord> csvRecords) {
    // Verify the inputs.
    Objects.requireNonNull(csvRecords);
    if (csvRecords.isEmpty()) {
      throw new IllegalArgumentException();
    }
    lines = csvRecords;
    header = lines.get(0);
  }

  /**
   * Simplified constructor for cases where this is only a single line of data.
   *
   * @param singleLine one line of CSV data
   */
  public RifObjectWrapper(CSVRecord singleLine) {
    lines = ImmutableList.of(singleLine);
    header = singleLine;
  }

  /**
   * Accessor for number of lines.
   *
   * @return number of lines
   */
  public int getLinesCount() {
    return lines.size();
  }

  /**
   * Accessor for a line by index.
   *
   * @param index 0 based index
   * @return wrapper for the line at the given index
   */
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
    if (header.isMapped(label)) {
      // header.get() returns an error if the field is missing
      return !Strings.isNullOrEmpty(header.get(label));
    }
    return false;
  }

  /**
   * Returns a (possibly empty) value for {@code label}.
   *
   * @param label the column label for the field to be checked
   * @param defaultValue the default value
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
