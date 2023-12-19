package gov.cms.bfd.model.codebook.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a grouped {@link List} of {@link Value}s in a {@link Variable}. These {@link
 * ValueGroup}ings are only used to associate a common {@link #getDescription()} value with related
 * {@link Variable}s.
 *
 * <p>Note that many {@link ValueGroup}s do not have a description. This is the case for {@link
 * Variable}s that only have a single {@link ValueGroup}.
 *
 * <p>Note that only some {@link Variable}s are coded, and only some of those coded {@link
 * Variable}s have their possible {@link Value}s enumerated in the {@link Codebook}. For {@link
 * Variable}s that don't have their possible {@link Value}s enumerated in the {@link Codebook}, the
 * {@link Variable#getValueGroups()} property will be <code>null</code>.
 */
@XmlAccessorType(XmlAccessType.PROPERTY)
public final class ValueGroup {
  /**
   * A textual description that applies to all of the {@link Variable}s in this {@link ValueGroup},
   * with one paragraph per {@link List} entry, or {@code null} if there is no such description
   * (which will only be the case for {@link Variable}s with a single {@link ValueGroup}).
   */
  private List<String> description;

  /** The mutable {@link List} of {@link Value}s in this {@link ValueGroup}. */
  private final List<Value> values;

  /** Constructs a new {@link ValueGroup}. */
  public ValueGroup() {
    this.description = null;
    this.values = new ArrayList<>();
  }

  /**
   * Constructs a new {@link ValueGroup} instance for the CCWCodebookMissingVariable. Had to add new
   * constructor for instantiation of the CCWCodebookInterface
   *
   * @param description the description
   * @param values the values
   */
  public ValueGroup(List<String> description, List<Value> values) {
    this.description = description;
    this.values = values;
  }

  /**
   * Gets the {@link #description}.
   *
   * @return a textual description that applies to all of the {@link Variable}s in this {@link
   *     ValueGroup}, with one paragraph per {@link List} entry, or {@code null} if there is no such
   *     description (which will only be the case for {@link Variable}s with a single {@link
   *     ValueGroup})
   */
  @XmlElementWrapper(required = false)
  @XmlElement(name = "p")
  public List<String> getDescription() {
    return description;
  }

  /**
   * Sets the {@link #description}.
   *
   * @param description the new value to use for {@link #getDescription()}
   */
  public void setDescription(List<String> description) {
    this.description = description;
  }

  /**
   * Gets the {@link #values}.
   *
   * @return the mutable {@link List} of {@link Value}s in this {@link ValueGroup}
   */
  @XmlElement(name = "value")
  public List<Value> getValues() {
    return values;
  }
}
