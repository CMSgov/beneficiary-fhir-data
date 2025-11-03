package gov.cms.bfd.server.ng;

import gov.cms.bfd.server.ng.util.IdrConstants;
import gov.cms.bfd.server.ng.util.SystemUrls;
import org.hl7.fhir.r4.model.Coding;

/** Represents Claim Security status. */
public enum ClaimSecurityStatus {

  /** No special security tags are required for this claim. */
  NONE,

  /** This claim requires SAMHSA tags. */
  SAMHSA_APPLICABLE;

  /**
   * Creates Security tag coding.
   *
   * @param securityStatus securityStatus
   * @return coding
   */
  public static Coding toFhir(ClaimSecurityStatus securityStatus) {

    var coding = new Coding();
    if (securityStatus == ClaimSecurityStatus.SAMHSA_APPLICABLE) {
      coding
          .setSystem(SystemUrls.SAMHSA_ACT_CODE_SYSTEM_URL)
          .setCode(IdrConstants.SAMHSA_SECURITY_CODE)
          .setDisplay(IdrConstants.SAMHSA_SECURITY_DISPLAY);
    }
    return coding;
  }
}
