package gov.cms.bfd.server.ng.beneficiary.model;

/**
 * Filter class to apply via SQL restriction to places that need to delimit valid beneficiaries from
 * the beneficiary table.
 */
public final class ValidBeneficiary {

  private ValidBeneficiary() {}

  /** SQL Restriction statement needed to limit to valid beneficiaries. */
  public static final String SQL_RESTRICTION =
      """
      NOT EXISTS (
          SELECT 1 FROM idr.beneficiary_overshare_mbi om
          WHERE om.bene_mbi_id::text = bene_mbi_id::text
      )
      """;

  /** SQL QUERY representation of valid beneficiaries. */
  public static final String VIEW =
      """
         SELECT *
         FROM idr.beneficiary b
         WHERE NOT EXISTS(SELECT 1 FROM idr.beneficiary_overshare_mbi om WHERE om.bene_mbi_id = b.bene_mbi_id)
       """;
}
