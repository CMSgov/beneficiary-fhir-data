package gov.cms.bfd.server.war.commons;

/** An enum encapsulating a variety of common codings. */
public enum CommonCodings {

  /** The medicare number. */
  MC(TransformerConstants.CODING_SYSTEM_HL7_IDENTIFIER_TYPE, "MC", "Patient's Medicare Number");

  /** The coding's system. */
  private final String system;

  /** The coding's code. */
  private final String code;

  /** The coding's display. */
  private final String display;

  /**
   * Instantiates a new Common codings.
   *
   * @param system the system
   * @param code the code
   * @param display the display
   */
  CommonCodings(String system, String code, String display) {
    this.system = system;
    this.code = code;
    this.display = display;
  }

  /**
   * Gets the {@link #system}.
   *
   * @return the system
   */
  public String getSystem() {
    return system;
  }

  /**
   * Gets the {@link #code}.
   *
   * @return the code
   */
  public String getCode() {
    return code;
  }

  /**
   * Gets the {@link #display}.
   *
   * @return the display
   */
  public String getDisplay() {
    return display;
  }
}
