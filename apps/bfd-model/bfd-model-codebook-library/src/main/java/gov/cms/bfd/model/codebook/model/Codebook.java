package gov.cms.bfd.model.codebook.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;

/**
 * Represents the data contained in a <a
 * href="https://www.ccwdata.org/web/guest/data-dictionaries">CMS Chronic Conditions Warehouse (CCW)
 * data dictionary</a> codebook.
 */
@Getter
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public final class Codebook {
  /** The short identifier for this {@link Codebook}, for use in debugging. */
  @XmlAttribute private final String id;

  /** The descriptive English name for this {@link Codebook}. */
  @XmlAttribute private final String name;

  /**
   * A human-readable {@link String} that identifies which version of the data is represented by
   * this {@link Codebook}, typically something like "<code>December 2042, Version 42.0</code>".
   */
  @XmlAttribute private final String version;

  /** The mutable {@link List} of {@link Variable}s in the {@link Codebook}. */
  @XmlElement(name = "variable")
  private final List<Variable> variables;

  /**
   * Constructs a new {@link Codebook}.
   *
   * @param codebookSource the {@link SupportedCodebook} that this {@link Codebook} is being built
   *     from
   */
  public Codebook(SupportedCodebook codebookSource) {
    this.id = codebookSource.name();
    this.name = codebookSource.getDisplayName();
    this.version = codebookSource.getVersion();
    this.variables = new ArrayList<>();
  }

  /** This public no-arg constructor is required by JAXB. */
  @Deprecated
  public Codebook() {
    this.id = null;
    this.name = null;
    this.version = null;
    this.variables = new ArrayList<>();
  }

  /** {@inheritDoc} */
  @Override
  public String toString() {
    return getId();
  }
}
