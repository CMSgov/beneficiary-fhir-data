package gov.cms.bfd.model.rda;

/**
 * Used to mark entities that contain phase information. Allows them to be recognized in API when
 * filtering based on phase.
 */
public interface EntityWithPhaseNumber {
  /**
   * Identifies the particular phase associated with this claim (1, 2, 3).
   *
   * @return the phase number or null
   */
  Short getPhase();

  /**
   * The phase sequence number.
   *
   * @return the phase sequence number or null
   */
  Short getPhaseSeqNum();
}
