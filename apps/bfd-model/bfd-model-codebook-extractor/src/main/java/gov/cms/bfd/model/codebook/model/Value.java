package gov.cms.bfd.model.codebook.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlValue;

/**
 * Each {@link Value} instance in a {@link ValueGroup} represents one of the allowed coded values
 * for its grandparent {@link Variable}.
 *
 * <p>Note that only some {@link Variable}s are coded, and only some of those coded {@link
 * Variable}s have their possible {@link Value}s enumerated in the {@link Codebook}.
 */
@XmlAccessorType(XmlAccessType.PROPERTY)
public final class Value {
  private String code;
  private String description;

  /** Constructs a new {@link Value} instance. */
  public Value() {
    this.code = null;
    this.description = null;
  }

  /**
   * Constructs a new {@link Value} instance for the CCWCodebookMissingVariable. Had to add new
   * constructor for instantiation of the CCWCodebookInterface
   *
   * @param code the code
   * @param description the description
   */
  public Value(String code, String description) {
    this.code = code;
    this.description = description;
  }

  /**
   * Gets the code.
   *
   * @return the coded value for this {@link Value}, representing one of the allowed values for its
   *     great-grandparent {@link Variable}
   */
  @XmlAttribute
  public String getCode() {
    return code;
  }

  /**
   * Sets the code.
   *
   * @param code the new value for {@link #getCode()}
   */
  public void setCode(String code) {
    this.code = code;
  }

  /**
   * Gets the description.
   *
   * @return a brief English human-readable description of this {@link Value}
   */
  @XmlValue
  public String getDescription() {
    return description;
  }

  /**
   * Sets the description.
   *
   * @param description the new value for {@link #getDescription()}
   */
  public void setDescription(String description) {
    this.description = description;
  }

  /** @see java.lang.Object#toString() */
  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("Value [code=");
    builder.append(code);
    builder.append(", description=");
    builder.append(description);
    builder.append("]");
    return builder.toString();
  }
}
