package gov.cms.bfd.model.codebook.model;

import java.util.List;
import java.util.Optional;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlTransient;

/**
 * Each {@link Variable} instance represents one of the CCW variables/fields documented in a {@link
 * Codebook}.
 */
@XmlAccessorType(XmlAccessType.FIELD)
public final class Variable {
  @XmlTransient private Codebook codebook;

  @XmlAttribute(required = true)
  private String id;

  @XmlAttribute(required = true)
  private String label;

  @XmlElementWrapper(required = false)
  @XmlElement(name = "p")
  private List<String> description;

  @XmlAttribute(required = false)
  private String shortName;

  @XmlAttribute(required = true)
  private String longName;

  @XmlAttribute(required = false)
  private VariableType type;

  @XmlAttribute(required = true)
  private Integer length;

  @XmlAttribute(required = false)
  private String source;

  @XmlAttribute(required = false)
  private String valueFormat;

  @XmlElementWrapper(required = false)
  @XmlElement(name = "valueGroup")
  private List<ValueGroup> valueGroups;

  @XmlElementWrapper(required = false)
  @XmlElement(name = "p")
  private List<String> comment;

  /**
   * Constructs a new {@link Variable} instance.
   *
   * @param codebook the value to use for {@link #getCodebook()}
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
   * Gets the codebook.
   *
   * @return the parent {@link Codebook} that this {@link Variable} is part of
   */
  public Codebook getCodebook() {
    return codebook;
  }

  /**
   * Gets the id.
   *
   * @return the unique-within-a-{@link Codebook} identifier for this {@link Variable}
   */
  public String getId() {
    return id;
  }

  /**
   * Sets the id.
   *
   * @param id the new value for {@link #getId()}
   */
  public void setId(String id) {
    this.id = id;
  }

  /**
   * Gets the label.
   *
   * @return a short description for this {@link Variable}, typically no more than a few (English)
   *     words long
   */
  public String getLabel() {
    return label;
  }

  /**
   * Sets the label.
   *
   * @param label the new value for {@link #getLabel()}
   */
  public void setLabel(String label) {
    this.label = label;
  }

  /**
   * Gets the description.
   *
   * @return a longer description for this {@link Variable}, typically one or more (English)
   *     paragraphs long, with one {@link List} entry per paragraph
   */
  public Optional<List<String>> getDescription() {
    return Optional.ofNullable(description);
  }

  /**
   * Sets the description.
   *
   * @param description the new value for {@link #getDescription()}
   */
  public void setDescription(List<String> description) {
    this.description = description;
  }

  /**
   * Gets the short name.
   *
   * @return the "short" name for this {@link Variable}, which will be unique-within-this-{@link
   *     Codebook} and to identify it by some systems, or <code>null</code> if that information is
   *     unknown for this {@link Variable}
   */
  public Optional<String> getShortName() {
    return Optional.ofNullable(shortName);
  }

  /**
   * Sets the short name.
   *
   * @param shortName the new value for {@link #getShortName()}
   */
  public void setShortName(String shortName) {
    this.shortName = shortName;
  }

  /**
   * Gets the long name.
   *
   * @return the "long" name for this {@link Variable}, which will be unique-within-this-{@link
   *     Codebook} and to identify it by some systems
   */
  public String getLongName() {
    return longName;
  }

  /**
   * Sets the long name.
   *
   * @param longName the new value for {@link #getLongName()}
   */
  public void setLongName(String longName) {
    this.longName = longName;
  }

  /**
   * Gets the type.
   *
   * @return the {@link VariableType} that constrains values of this {@link Variable}, or <code>
   *     null     </code> if that information is unknown for this {@link Variable}
   */
  public Optional<VariableType> getType() {
    return Optional.ofNullable(type);
  }

  /**
   * Sets the type.
   *
   * @param type the new value for {@link #getType()}
   */
  public void setType(VariableType type) {
    this.type = type;
  }

  /**
   * Gets the length.
   *
   * @return the maximum length that constrains values of this {@link Variable}
   */
  public Integer getLength() {
    return length;
  }

  /**
   * Sets the length.
   *
   * @param length the new value for {@link #getLength()}
   */
  public void setLength(Integer length) {
    this.length = length;
  }

  /**
   * Gets the source.
   *
   * @return the source system that this {@link Variable}'s data was extracted and/or derived from,
   *     or <code>null</code> if that information is unknown for this {@link Variable}
   */
  public Optional<String> getSource() {
    return Optional.ofNullable(source);
  }

  /**
   * Sets the source.
   *
   * @param source the new value for {@link #getSource()}
   */
  public void setSource(String source) {
    this.source = source;
  }

  /**
   * Gets the value format.
   *
   * @return the descriptive text that details the format of the {@link Variable}'s values, e.g.
   *     "XXX.XX" or "The value in this field is between '00' through '12'.", or <code>null</code>
   *     if no such description is available
   */
  public Optional<String> getValueFormat() {
    return Optional.ofNullable(valueFormat);
  }

  /**
   * Sets the value format.
   *
   * @param valueFormat the new value for {@link #getValueFormat()}
   */
  public void setValueFormat(String valueFormat) {
    this.valueFormat = valueFormat;
  }

  /**
   * Gets the value groups.
   *
   * @return the {@link List} of {@link ValueGroup}s that constrains the allowed coded values of
   *     this {@link Variable}, or <code>null</code> if this {@link Variable}'s values aren't
   *     constrained in that way
   */
  public Optional<List<ValueGroup>> getValueGroups() {
    return Optional.ofNullable(valueGroups);
  }

  /**
   * Sets the value groups.
   *
   * @param valueGroups the new value for {@link #getValueGroups()}
   */
  public void setValueGroups(List<ValueGroup> valueGroups) {
    this.valueGroups = valueGroups;
  }

  /**
   * Gets the comment.
   *
   * @return a comment providing more detail on this {@link Variable}'s purpose and/or history, zero
   *     or more (English) paragraphs long, with one {@link List} entry per paragraph, or <code>null
   *     </code> if that information is unknown for this {@link Variable}
   */
  public Optional<List<String>> getComment() {
    return Optional.ofNullable(comment);
  }

  /** @param comment the new value for {@link #getComment()} */
  public void setComment(List<String> comment) {
    this.comment = comment;
  }

  /** @see java.lang.Object#toString() */
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
