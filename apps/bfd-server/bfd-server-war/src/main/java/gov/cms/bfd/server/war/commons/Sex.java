package gov.cms.bfd.server.war.commons;

import gov.cms.bfd.model.rif.entities.Beneficiary;

/** Enumerates the value codeset indicating the sex of the {@link Beneficiary}. */
public enum Sex {
  /** Represents an unknown sex. */
  UNKNOWN('0'),
  /** Represents the male sex. */
  MALE('1'),
  /** Represents the female sex. */
  FEMALE('2');

  /** The code representing the sex of the {@link Beneficiary}. */
  private char code;

  /**
   * Gets the {@link #code}.
   *
   * @return the code
   */
  public char getCode() {
    return this.code;
  }

  /**
   * Instantiates a new {@link Sex}.
   *
   * @param code the code
   */
  private Sex(char code) {
    this.code = code;
  }
}
