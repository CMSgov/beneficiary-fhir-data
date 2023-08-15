package gov.cms.bfd.model.codebook.model;

/** The interface for CCW Codebook variables. */
public interface CcwCodebookInterface {

  /**
   * Gets the variable.
   *
   * @return the variable
   */
  public Variable getVariable();

  /**
   * Gets the CCW Codebook variable's name.
   *
   * @return the name
   */
  public String name();
}
