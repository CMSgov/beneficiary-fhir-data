package gov.cms.bfd.model.rda;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.google.common.base.Strings;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import lombok.Data;

/**
 * A wrapper for a {@link List} of string values. Jackson maps instances of this class to and from
 * simple JSON arrays of strings.
 */
@Data
public class StringList {
  @JsonValue private List<String> values = new ArrayList<>();

  /**
   * Produces a new instance containing all of the values in the provided array.
   *
   * @param values array of values for the new list
   * @return the new list
   */
  @JsonCreator
  public static StringList of(String... values) {
    var list = new StringList();
    Stream.of(values).forEach(list::add);
    return list;
  }

  /**
   * Produces a new instance containing all of the non-null, non-empty values in the provided array.
   *
   * @param values array of potential values for the new list
   * @return the new list
   */
  public static StringList ofNonEmpty(String... values) {
    var list = new StringList();
    Stream.of(values).forEach(list::addIfNonEmpty);
    return list;
  }

  /**
   * Add the value converted to a String to our list if it is non-null.
   *
   * @param value potential value to be added
   * @return this instance
   */
  public StringList addIfNonNull(Character value) {
    if (value != null) {
      values.add(value.toString());
    }
    return this;
  }

  /**
   * Add the value to our list if it is non-null and non-empty.
   *
   * @param value potential value to be added
   * @return this instance
   */
  public StringList addIfNonEmpty(String value) {
    if (!Strings.isNullOrEmpty(value)) {
      values.add(value);
    }
    return this;
  }

  /**
   * Add the value to our list.
   *
   * @param value value to be added
   * @return this instance
   */
  public StringList add(String value) {
    values.add(value);
    return this;
  }

  @Override
  public String toString() {
    return values.toString();
  }
}
