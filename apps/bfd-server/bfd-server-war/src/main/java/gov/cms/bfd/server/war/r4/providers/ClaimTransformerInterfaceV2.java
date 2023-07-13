package gov.cms.bfd.server.war.r4.providers;

import org.hl7.fhir.r4.model.ExplanationOfBenefit;

/** Interface defining a transform method for claim processing. */
public interface ClaimTransformerInterfaceV2 {
  /**
   * Defines interface method to transform claims that do not require processing of an NPI tax ID
   * number; those claims are: {@link ClaimTypeV2.HHA}. {@link ClaimTypeV2.HOSPICE}. {@link
   * ClaimTypeV2.INPATIENT}. {@link ClaimTypeV2.OUTPATIENT}. {@link ClaimTypeV2.PDE}. {@link
   * ClaimTypeV2.SNF}.
   *
   * <p>Will throw {@link BadCodeMonkeyException} if this transformer is used for processing {@link
   * ClaimTypeV2.CARRIER} or {@link ClaimTypeV2.DME} claims.
   *
   * @param claimEntity generic entity {@link Object} associated with a claim type.
   * @return {@link ExplanationOfBenefit} claims for
   */
  ExplanationOfBenefit transform(Object claimEntity);

  /**
   * Defines interface method to transform claims that do require processing of an NPI tax ID
   * number.; those claims are: {@link ClaimTypeV2.CARRIER}. {@link ClaimTypeV2.DME}.
   *
   * <p>Will throw {@link BadCodeMonkeyException} if this transformer is used for processing {@link
   * ClaimTypeV2.HHA}, {@link ClaimTypeV2.HOSPICE}, {@link ClaimTypeV2.INPATIENT}, {@link
   * ClaimTypeV2.OUTPATIENT}, {@link ClaimTypeV2.PDE} or {@link ClaimTypeV2.SNF} claims.
   *
   * @param claimEntity generic entity {@link Object} associated with a claim type.
   * @param includeTaxNumbers {@link boolean} denoting inclusion of tax numbers.
   * @return {@link ExplanationOfBenefit}
   */
  ExplanationOfBenefit transform(Object claimEntity, boolean includeTaxNumbers);
}
