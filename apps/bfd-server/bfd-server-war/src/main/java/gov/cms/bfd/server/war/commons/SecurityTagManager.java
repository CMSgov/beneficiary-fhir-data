package gov.cms.bfd.server.war.commons;

import gov.cms.bfd.sharedutils.TagCode;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.hl7.fhir.r4.model.Coding;
import org.springframework.stereotype.Service;

/** Find security level. */
@Service
public final class SecurityTagManager {

  /**
   * Determines the security level based on the collected tags.
   *
   * <p>// * @param claimId value of claimId
   *
   * @param securityTags value of tagClass
   * @return SecurityLevel
   */
  public List<Coding> getClaimSecurityLevel(Set<String> securityTags) {
    List<Coding> securityTagCoding = new ArrayList<>();

    if (securityTags.isEmpty()) {
      addDefaultSecurityTag(securityTagCoding);
    } else {
      for (String securityTag : securityTags) {
        addSecurityTagCoding(securityTag, securityTagCoding);
      }
    }

    return securityTagCoding;
  }

  private void addDefaultSecurityTag(List<Coding> securityTagCoding) {
    Coding coding = new Coding();
    coding
        .setSystem(TransformerConstants.SAMHSA_CONFIDENTIALITY_CODE_SYSTEM_URL)
        .setCode("N")
        .setDisplay("Normal");
    securityTagCoding.add(coding);
  }

  private void addSecurityTagCoding(String securityTag, List<Coding> securityTagCoding) {
    Coding coding = new Coding();
    TagCode tagCode = TagCode.fromString(securityTag);

    if (tagCode != null) {
      switch (tagCode) {
        case R:
          coding
              .setSystem(TransformerConstants.SAMHSA_CONFIDENTIALITY_CODE_SYSTEM_URL)
              .setCode(TagCode.R.toString())
              .setDisplay(TagCode.R.getDisplayName());
          break;
        case _42CFRPart2:
          coding
              .setSystem(TransformerConstants.SAMHSA_ACT_CODE_SYSTEM_URL)
              .setCode(TagCode._42CFRPart2.toString())
              .setDisplay(TagCode._42CFRPart2.getDisplayName());
          break;
      }
    }

    securityTagCoding.add(coding);
  }

  /**
   * Determines the security level based on the collected tags.
   *
   * @param securityTags value of claimEntity
   * @return SecurityLevel
   */
  public List<org.hl7.fhir.dstu3.model.Coding> getClaimSecurityLevelDstu3(
      Set<String> securityTags) {
    List<Coding> coding = getClaimSecurityLevel(securityTags);

    List<org.hl7.fhir.dstu3.model.Coding> securityTagCoding = new ArrayList<>();

    for (Coding code : coding) {
      org.hl7.fhir.dstu3.model.Coding securityTag = new org.hl7.fhir.dstu3.model.Coding();
      securityTag
          .setSystem(TransformerConstants.SAMHSA_CONFIDENTIALITY_CODE_SYSTEM_URL)
          .setCode(code.getCode())
          .setDisplay(code.getDisplay());

      securityTagCoding.add(securityTag);
    }
    return securityTagCoding;
  }
}
