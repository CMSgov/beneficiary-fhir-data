package gov.cms.bfd.model.codebook.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlValue;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Each {@link Value} instance in a {@link ValueGroup} represents one of the allowed coded values
 * for its grandparent {@link Variable}.
 *
 * <p>Note that only some {@link Variable}s are coded, and only some of those coded {@link
 * Variable}s have their possible {@link Value}s enumerated in the {@link Codebook}.
 */
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@XmlAccessorType(XmlAccessType.PROPERTY)
public final class Value {

  /**
   * The coded value for this {@link Value}, representing one of the allowed values for its
   * great-grandparent {@link Variable}.
   */
  private String code;

  /** A brief English human-readable description of this {@link Value}. */
  private String description;

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
   * Gets the description.
   *
   * @return a brief English human-readable description of this {@link Value}
   */
  @XmlValue
  public String getDescription() {
    return description;
  }
}
