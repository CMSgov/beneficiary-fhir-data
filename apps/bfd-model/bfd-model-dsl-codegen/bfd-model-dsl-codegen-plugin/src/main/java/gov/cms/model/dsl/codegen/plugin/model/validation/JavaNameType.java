package gov.cms.model.dsl.codegen.plugin.model.validation;

import java.util.regex.Pattern;
import lombok.Getter;

/** Used to define the type of java name to be validated. */
public enum JavaNameType {
  /**
   * Just a plain identifier usable as a variable or field name. Simple identifiers consist of a
   * letter or underscore followed by zero or more letter, digit, or underscore characters.
   */
  Simple(ValidationUtil.SimpleJavaIdRegex),
  /**
   * Either a simple identifier or a pair of simple identifiers separated by a period. Used to allow
   * use of a single level compound property accessor.
   */
  Property(ValidationUtil.SimpleJavaIdRegex + "(\\." + ValidationUtil.SimpleJavaIdRegex + ")?"),
  /** One or more simple identifiers separated by periods to form a class or interface name. */
  Compound(ValidationUtil.SimpleJavaIdRegex + "(\\." + ValidationUtil.SimpleJavaIdRegex + ")*");

  /** {@link Pattern} used to validate strings. */
  @Getter private final Pattern regex;

  /**
   * Constructor for instances.
   *
   * @param regex regular expression used to validate strings
   */
  JavaNameType(String regex) {
    this.regex = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
  }
}
