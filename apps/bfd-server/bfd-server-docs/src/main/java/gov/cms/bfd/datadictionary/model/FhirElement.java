package gov.cms.bfd.datadictionary.model;

/** Data class for serializing data dictionary FHIR element resource files. */
public class FhirElement {
  /** Id. */
  private int id;

  /** Name. */
  private String name;

  /** Description. */
  private String description;

  /** String[] Applies To. */
  private String[] appliesTo;

  /** String[] Supplied In. */
  private String[] suppliedIn;

  /** BFD Database Table Type. */
  private String bfdTableType;

  /** BFD Database Column Name. */
  private String bfdColumnName;

  /** BFD Database Type. */
  private String bfdDbType;

  /** BFD Database Column Size. */
  private Integer bfdDbSize;

  /** BFD Java Field Name. */
  private String bfdJavaFieldName;

  /** String[] of CCW Mappings. */
  private String[] ccwMapping;

  /** String[] of CCLF Mappings. */
  private String[] cclfMapping;

  /** Array of FHIR Mappings. */
  private FhirMapping[] fhirMapping;

  /**
   * Retrieve Id.
   *
   * @return int value of id
   */
  public int getId() {
    return id;
  }

  /**
   * Set the Id.
   *
   * @param id int value to set
   */
  public void setId(int id) {
    this.id = id;
  }

  /**
   * Retrieve name.
   *
   * @return String value of name
   */
  public String getName() {
    return name;
  }

  /**
   * Set the name.
   *
   * @param name String value to set
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * Retrieve the description.
   *
   * @return String value of the description
   */
  public String getDescription() {
    return description;
  }

  /**
   * Set the description.
   *
   * @param description String value to set
   */
  public void setDescription(String description) {
    this.description = description;
  }

  /**
   * Retrieve the values of appliesTo.
   *
   * @return an array of Strings
   */
  public String[] getAppliesTo() {
    return appliesTo;
  }

  /**
   * Set the value of appliesTo.
   *
   * @param appliesTo a String[] value to set
   */
  public void setAppliesTo(String[] appliesTo) {
    this.appliesTo = appliesTo;
  }

  /**
   * Retrieve the values of suppliedIn.
   *
   * @return an array of Strings
   */
  public String[] getSuppliedIn() {
    return suppliedIn;
  }

  /**
   * Set the value of suppliedIn.
   *
   * @param suppliedIn a String[] value to set
   */
  public void setSuppliedIn(String[] suppliedIn) {
    this.suppliedIn = suppliedIn;
  }

  /**
   * Retrieve the BFD table type.
   *
   * @return a String value of the table type
   */
  public String getBfdTableType() {
    return bfdTableType;
  }

  /**
   * Set the value of BFD table type.
   *
   * @param bfdTableType the String value to set
   */
  public void setBfdTableType(String bfdTableType) {
    this.bfdTableType = bfdTableType;
  }

  /**
   * Retrieve the BFD column name.
   *
   * @return a String value of the column name
   */
  public String getBfdColumnName() {
    return bfdColumnName;
  }

  /**
   * Set the value of the BFD column name.
   *
   * @param bfdColumnName the String value to set
   */
  public void setBfdColumnName(String bfdColumnName) {
    this.bfdColumnName = bfdColumnName;
  }

  /**
   * Retrieve the BFD database type.
   *
   * @return a String value of the database type
   */
  public String getBfdDbType() {
    return bfdDbType;
  }

  /**
   * Set the value of the BFD database type.
   *
   * @param bfdDbType the String value to set
   */
  public void setBfdDbType(String bfdDbType) {
    this.bfdDbType = bfdDbType;
  }

  /**
   * Retrieve the BFD database size.
   *
   * @return an Integer value of the database size
   */
  public Integer getBfdDbSize() {
    return bfdDbSize;
  }

  /**
   * Set the BFD database size.
   *
   * @param bfdDbSize the Integer value to set
   */
  public void setBfdDbSize(Integer bfdDbSize) {
    this.bfdDbSize = bfdDbSize;
  }

  /**
   * Retrieve the BFD java field name.
   *
   * @return a String value of the java field name
   */
  public String getBfdJavaFieldName() {
    return bfdJavaFieldName;
  }

  /**
   * Set the BFD java field name.
   *
   * @param bfdJavaFieldName a String value to set
   */
  public void setBfdJavaFieldName(String bfdJavaFieldName) {
    this.bfdJavaFieldName = bfdJavaFieldName;
  }

  /**
   * Retrieve the CCW mappings.
   *
   * @return a String[] of CCW mappings
   */
  public String[] getCcwMapping() {
    return ccwMapping;
  }

  /**
   * Set the CCW mappings.
   *
   * @param ccwMapping the String[] to set
   */
  public void setCcwMapping(String[] ccwMapping) {
    this.ccwMapping = ccwMapping;
  }

  /**
   * Retrieve the CCLF mappings.
   *
   * @return a String[] of CCLF mappings
   */
  public String[] getCclfMapping() {
    return cclfMapping;
  }

  /**
   * Set the CCLF mappings.
   *
   * @param cclfMapping the String[] to set.
   */
  public void setCclfMapping(String[] cclfMapping) {
    this.cclfMapping = cclfMapping;
  }

  /**
   * Retrieve the FHIR mappings.
   *
   * @return an array of FhirMapping values
   */
  public FhirMapping[] getFhirMapping() {
    return fhirMapping;
  }

  /**
   * Set the FHIR mappings.
   *
   * @param fhirMapping an array of FhirMapping to set
   */
  public void setFhirMapping(FhirMapping[] fhirMapping) {
    this.fhirMapping = fhirMapping;
  }
}
