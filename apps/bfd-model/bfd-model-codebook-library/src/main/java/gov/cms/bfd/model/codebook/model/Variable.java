package gov.cms.bfd.model.codebook.model;

import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlTransient;
import java.util.List;
import java.util.Optional;
import lombok.Getter;
import lombok.Setter;

/**
 * Each {@link Variable} instance represents one of the CCW variables/fields documented in a {@link
 * Codebook}.
 */
@XmlAccessorType(XmlAccessType.FIELD)
public final class Variable {
  /** The parent {@link Codebook} that this {@link Variable} is part of. */
  @Getter @XmlTransient private Codebook codebook;

  /** The unique-within-a-{@link Codebook} identifier for this {@link Variable}. */
  @Getter
  @Setter
  @XmlAttribute(required = true)
  private String id;

  /**
   * A short description for this {@link Variable}, typically no more than a few (English) words
   * long.
   */
  @Setter
  @Getter
  @XmlAttribute(required = true)
  private String label;

  /**
   * A longer description for this {@link Variable}, typically one or more (English) paragraphs
   * long, with one {@link List} entry per paragraph.
   */
  @Setter
  @XmlElementWrapper(required = false)
  @XmlElement(name = "p")
  private List<String> description;

  /**
   * The "short" name for this {@link Variable}, which will be unique-within-this-{@link Codebook}
   * and to identify it by some systems. Will be {@code null} if that information is unknown for
   * this {@link Variable}.
   */
  @Setter
  @XmlAttribute(required = false)
  private String shortName;

  /**
   * The "long" name for this {@link Variable}, which will be unique-within-this-{@link Codebook}
   * and to identify it by some systems.
   */
  @Setter
  @Getter
  @XmlAttribute(required = true)
  private String longName;

  /**
   * The {@link VariableType} that constrains values of this {@link Variable}. Will be {@code null}
   * if that information is unknown for this {@link Variable}.
   */
  @Setter
  @XmlAttribute(required = false)
  private VariableType type;

  /** The maximum length that constrains values of this {@link Variable}. */
  @Setter
  @Getter
  @XmlAttribute(required = true)
  private Integer length;

  /**
   * The source system that this {@link Variable}'s data was extracted and/or derived from. Will be
   * {@code null} if that information is unknown for this {@link Variable}.
   */
  @Setter
  @XmlAttribute(required = false)
  private String source;

  /**
   * The descriptive text that details the format of the {@link Variable}'s values, e.g. "XXX.XX" or
   * "The value in this field is between '00' through '12'.". Will be {@code null} if no such
   * description is available.
   */
  @Setter
  @XmlAttribute(required = false)
  private String valueFormat;

  /**
   * The {@link List} of {@link ValueGroup}s that constrains the allowed coded values of this {@link
   * Variable}. Will be {@code null} if this {@link Variable}'s values aren't constrained in that
   * way.
   */
  @Setter
  @XmlElementWrapper(required = false)
  @XmlElement(name = "valueGroup")
  private List<ValueGroup> valueGroups;

  /**
   * A comment providing more detail on this {@link Variable}'s purpose and/or history, zero or more
   * (English) paragraphs long, with one {@link List} entry per paragraph. Will be {@code null} if
   * that information is unknown for this {@link Variable}.
   */
  @Setter
  @XmlElementWrapper(required = false)
  @XmlElement(name = "p")
  private List<String> comment;

  /**
   * Constructs a new {@link Variable} instance.
   *
   * @param codebook the value to use for getCodebook()
   */
  public Variable(Codebook codebook) {
    this.codebook = codebook;

    this.id = null;
    this.label = null;
    this.description = null;
    this.shortName = null;
    this.longName = null;
    this.type = null;
    this.length = null;
    this.source = null;
    this.valueFormat = null;
    this.valueGroups = null;
    this.comment = null;
  }

  /** This public no-arg constructor is required by JAXB. */
  @Deprecated
  public Variable() {
    this.codebook = null;
    this.id = null;
    this.label = null;
    this.description = null;
    this.shortName = null;
    this.longName = null;
    this.type = null;
    this.length = null;
    this.source = null;
    this.valueFormat = null;
    this.valueGroups = null;
    this.comment = null;
  }

  /**
   * Constructs a new {@link Variable} instance for the CCWCodebookMissingVariable. Has to add new
   * constructor for instantiation of the CCWCodebookInterface
   *
   * @param id the id
   * @param label the label
   * @param description the description
   * @param shortName the short name
   * @param longName the long name
   * @param type the type
   * @param length the length
   * @param source the source
   * @param valueFormat the value format
   * @param valueGroups the value groups
   * @param comment the comment
   */
  public Variable(
      String id,
      String label,
      List<String> description,
      String shortName,
      String longName,
      VariableType type,
      int length,
      String source,
      String valueFormat,
      List<ValueGroup> valueGroups,
      List<String> comment) {
    this.id = id;
    this.label = label;
    this.description = description;
    this.shortName = shortName;
    this.longName = longName;
    this.type = type;
    this.length = length;
    this.source = source;
    this.valueFormat = valueFormat;
    this.valueGroups = valueGroups;
    this.comment = comment;
  }

  /**
   * This listener method is called automagically by JAXB after each {@link Variable} instance is
   * unmarshalled. See "Unmarshall Event Callbacks" in the {@link Unmarshaller} JavaDocs for
   * details.
   *
   * @param unmarshaller the {@link Unmarshaller} being used
   * @param parent the parent XML element of this unmarshalled {@link Variable} instance
   */
  void afterUnmarshal(Unmarshaller unmarshaller, Object parent) {
    if (parent == null || !(parent instanceof Codebook)) throw new IllegalStateException();

    /*
     * The codebook field is marked @XmlTransient because its value is only implied
     * by the unmarshalling context -- it should be set to the parent Codebook that
     * each unmarshalled Variable was contained in.
     */
    this.codebook = (Codebook) parent;
  }

  /**
   * Gets the {@link #description}.
   *
   * @return a longer description for this {@link Variable}, typically one or more (English)
   *     paragraphs long, with one {@link List} entry per paragraph
   */
  public Optional<List<String>> getDescription() {
    return Optional.ofNullable(description);
  }

  /**
   * Gets the {@link #shortName}.
   *
   * @return the "short" name for this {@link Variable}, which will be unique-within-this-{@link
   *     Codebook} and to identify it by some systems, or <code>null</code> if that information is
   *     unknown for this {@link Variable}
   */
  public Optional<String> getShortName() {
    return Optional.ofNullable(shortName);
  }

  /**
   * Gets the {@link #type}.
   *
   * @return the {@link VariableType} that constrains values of this {@link Variable}, or <code>
   *     null     </code> if that information is unknown for this {@link Variable}
   */
  public Optional<VariableType> getType() {
    return Optional.ofNullable(type);
  }

  /**
   * Gets the {@link #source}.
   *
   * @return the source system that this {@link Variable}'s data was extracted and/or derived from,
   *     or <code>null</code> if that information is unknown for this {@link Variable}
   */
  public Optional<String> getSource() {
    return Optional.ofNullable(source);
  }

  /**
   * Gets the {@link #valueFormat}.
   *
   * @return the descriptive text that details the format of the {@link Variable}'s values, e.g.
   *     "XXX.XX" or "The value in this field is between '00' through '12'.", or <code>null</code>
   *     if no such description is available
   */
  public Optional<String> getValueFormat() {
    return Optional.ofNullable(valueFormat);
  }

  /**
   * Gets the {@link #valueGroups}.
   *
   * @return the {@link List} of {@link ValueGroup}s that constrains the allowed coded values of
   *     this {@link Variable}, or <code>null</code> if this {@link Variable}'s values aren't
   *     constrained in that way
   */
  public Optional<List<ValueGroup>> getValueGroups() {
    return Optional.ofNullable(valueGroups);
  }

  /**
   * Gets the {@link #comment}.
   *
   * @return a comment providing more detail on this {@link Variable}'s purpose and/or history, zero
   *     or more (English) paragraphs long, with one {@link List} entry per paragraph, or <code>null
   *     </code> if that information is unknown for this {@link Variable}
   */
  public Optional<List<String>> getComment() {
    return Optional.ofNullable(comment);
  }

  /** {@inheritDoc} */
  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("Variable [codebook=");
    builder.append(codebook);
    builder.append(", id=");
    builder.append(id);
    builder.append("]");
    return builder.toString();
  }
}
