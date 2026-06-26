package gov.cms.bfd.server.ng.audit;

import java.util.Optional;

/**
 * Search Criteria for AuditEvent searches by patient.
 *
 * @param beneSkId patientId
 * @param limit number of records to pull
 * @param lastIndex last index from previous page for pagination
 */
public record AuditPatientSearchCriteria(
    long beneSkId, Optional<Integer> limit, Optional<String> lastIndex) {

  /**
   * Returns the limit or the default.
   *
   * @return limit
   */
  public Integer resolveLimit() {
    return resolveLimitWithExtra(0);
  }

  /**
   * Returns the limit or the default.
   *
   * @param extra extra to add for pagination checking than the requested limit
   * @return limit
   */
  public Integer resolveLimitWithExtra(int extra) {
    return limit.orElse(5000) + extra;
  }
}
