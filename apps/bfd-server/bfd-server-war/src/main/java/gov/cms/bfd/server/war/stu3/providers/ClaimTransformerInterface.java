package gov.cms.bfd.server.war.stu3.providers;

import gov.cms.bfd.sharedutils.exceptions.BadCodeMonkeyException;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit;

/** Interface defining a transform method for claim processing. */
public interface ClaimTransformerInterface {
  /**
   * Defines interface method to transform claims that do not require processing of an NPI tax ID
   * number; those claims are: {@link ClaimType#HHA}. {@link ClaimType#HOSPICE}. {@link
   * ClaimType#INPATIENT}. {@link ClaimType#OUTPATIENT}. {@link ClaimType#PDE}. {@link
   * ClaimType#SNF}.
   *
   * <p>Will throw {@link BadCodeMonkeyException} if this transformer is used for processing {@link
   * ClaimType#CARRIER} or {@link ClaimType#DME} claims.
   *
   * @param claimEntity generic entity {@link Object} associated with a claim type.
   * @return {@link ExplanationOfBenefit} claims for
   */
  ExplanationOfBenefit transform(Object claimEntity);

  /**
   * Defines interface method to transform claims that do require processing of an NPI tax ID
   * number.; those claims are: {@link ClaimType#CARRIER}. {@link ClaimType#DME}.
   *
   * <p>Will throw {@link BadCodeMonkeyException} if this transformer is used for processing {@link
   * ClaimType#HHA}, {@link ClaimType#HOSPICE}, {@link ClaimType#INPATIENT}, {@link
   * ClaimType#OUTPATIENT}, {@link ClaimType#PDE} or {@link ClaimType#SNF} claims.
   *
   * @param claimEntity generic entity {@link Object} associated with a claim type.
   * @param includeTaxNumbers {@link boolean} denoting inclusion of tax numbers.
   * @return {@link ExplanationOfBenefit}
   */
  ExplanationOfBenefit transform(Object claimEntity, boolean includeTaxNumbers);
}
