package gov.cms.bfd.server.ng.loadprogress;

import java.util.List;

/**
 * Helper that centralizes which IDR tables are considered part of a resource for LoadProgress
 * checks.
 */
public final class LoadProgressTables {
  private LoadProgressTables() {}

  /**
   * Returns table-name prefixes to check for claim-related load progress entries.
   *
   * @return list of table-name prefixes associated with claim-related IDR tables
   */
  public static List<String> claimTablePrefixes() {
    return List.of(
        "idr.claim",
        "idr.claim_institutional",
        "idr.claim_date_signature",
        "idr.claim_item",
        "idr.claim_line_institutional",
        "idr.claim_ansi_signature",
        "idr.claim_professional",
        "idr.claim_line_professional");
  }

  /**
   * Returns table-name prefixes to check for beneficiary-related load progress entries.
   *
   * @return list of table-name prefixes associated with beneficiary-related IDR tables
   */
  public static List<String> beneficiaryTablePrefixes() {
    return List.of(
        "idr.beneficiary",
        "idr.beneficiary_mbi_id",
        "idr.beneficiary_status",
        "idr.beneficiary_third_party",
        "idr.beneficiary_entitlement",
        "idr.beneficiary_entitlement_reason",
        "idr.beneficiary_dual_eligibility",
        "idr.beneficiary_election_period_usage");
  }

  /**
   * Returns table-name prefixes to check for coverage-related load progress entries.
   *
   * @return list of table-name prefixes associated with coverage-related IDR tables
   */
  public static List<String> coverageTablePrefixes() {
    return List.of("idr.beneficiary_entitlement", "idr.beneficiary");
  }
}
