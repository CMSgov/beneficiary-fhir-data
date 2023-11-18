package gov.cms.bfd.datadictionary.model;

/** Data class for serializing FHIR mappings from data dictionary resource files. */
public class FhirMapping {
  /** Version. */
  private String version;

  /** Resource. */
  private String resource;

  /** Element. */
  private String element;

  /** FHIR Path. */
  private String fhirPath;

  /** String[] of discriminators. */
  private String[] discriminator;

  /** String[] of additional info. */
  private String[] additional;

  /** Derived. */
  private String derived;

  /** Note. */
  private String note;

  /** Example. */
  private String example;

  /**
   * Retrieve the version.
   *
   * @return a String value of the version
   */
  public String getVersion() {
    return version;
  }

  /**
   * Set the version.
   *
   * @param version a String value to set
   */
  public void setVersion(String version) {
    this.version = version;
  }

  /**
   * Retrieve the value of resource.
   *
   * @return a String value for resource
   */
  public String getResource() {
    return resource;
  }

  /**
   * Set the value of resource.
   *
   * @param resource a String value to set
   */
  public void setResource(String resource) {
    this.resource = resource;
  }

  /**
   * Retrieve the value of element.
   *
   * @return a String value for element
   */
  public String getElement() {
    return element;
  }

  /**
   * Set the value of element.
   *
   * @param element a String value to set.
   */
  public void setElement(String element) {
    this.element = element;
  }

  /**
   * Retrieve the FHIR path.
   *
   * @return a String value of the FHIR path
   */
  public String getFhirPath() {
    return fhirPath;
  }

  /**
   * Set the FHIR path.
   *
   * @param fhirPath a String value to set
   */
  public void setFhirPath(String fhirPath) {
    this.fhirPath = fhirPath;
  }

  /**
   * Retrieve discriminator values.
   *
   * @return a String[] of discriminator values
   */
  public String[] getDiscriminator() {
    return discriminator;
  }

  /**
   * Set the discriminator values.
   *
   * @param discriminator a String[] to set
   */
  public void setDiscriminator(String[] discriminator) {
    this.discriminator = discriminator;
  }

  /**
   * Retrieve the additional values.
   *
   * @return a String[] of additional values
   */
  public String[] getAdditional() {
    return additional;
  }

  /**
   * Set the additional values.
   *
   * @param additional a String[] to set
   */
  public void setAdditional(String[] additional) {
    this.additional = additional;
  }

  /**
   * Retrieve the value of derived.
   *
   * @return a String value of derived
   */
  public String getDerived() {
    return derived;
  }

  /**
   * Set the value of derived.
   *
   * @param derived a String value to set
   */
  public void setDerived(String derived) {
    this.derived = derived;
  }

  /**
   * Retrieve the value of note.
   *
   * @return a String value of note
   */
  public String getNote() {
    return note;
  }

  /**
   * Set the value of note.
   *
   * @param note a String value to set
   */
  public void setNote(String note) {
    this.note = note;
  }

  /**
   * Retrieve the value of example.
   *
   * @return a String value of example
   */
  public String getExample() {
    return example;
  }

  /**
   * Set the value of example.
   *
   * @param example a String value to set
   */
  public void setExample(String example) {
    this.example = example;
  }
}
