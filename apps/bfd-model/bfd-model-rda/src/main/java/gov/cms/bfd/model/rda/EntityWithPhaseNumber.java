package gov.cms.bfd.model.rda;

/**
 * Used to mark entities that contain phase information. Allows them to be recognized in API when
 * filtering based on phase.
 */
public interface EntityWithPhaseNumber {
  /** Name of the phase field for use in CriteriaQuery construction. */
  String PhaseFieldName = "phase";

  /**
   * Identifies the particular phase associated with this claim (1, 2, 3).
   *
   * @return the phase number or null
   */
  Short getPhase();
}
