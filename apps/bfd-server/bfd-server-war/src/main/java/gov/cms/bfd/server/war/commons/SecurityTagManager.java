package gov.cms.bfd.server.war.commons;

import gov.cms.bfd.model.rda.entities.RdaFissClaim;
import gov.cms.bfd.model.rda.entities.RdaMcsClaim;
import gov.cms.bfd.model.rif.entities.CarrierClaim;
import gov.cms.bfd.model.rif.entities.DMEClaim;
import gov.cms.bfd.model.rif.entities.HHAClaim;
import gov.cms.bfd.model.rif.entities.HospiceClaim;
import gov.cms.bfd.model.rif.entities.InpatientClaim;
import gov.cms.bfd.model.rif.entities.OutpatientClaim;
import gov.cms.bfd.model.rif.entities.PartDEvent;
import gov.cms.bfd.model.rif.entities.SNFClaim;
import gov.cms.bfd.sharedutils.TagCode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.hl7.fhir.r4.model.Coding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** Find security level. */
@Service
public final class SecurityTagManager {

  private static final Logger LOGGER = LoggerFactory.getLogger(SecurityTagManager.class);

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

  /**
   * Builds a mapping from claim IDs to their security tags.
   *
   * @param claimEntities the claim entity
   * @return set of ClaimIds
   */
  public Set<String> collectClaimIds(List<Object> claimEntities) {
    Set<String> claimIds = new HashSet<>();

    for (Object claimEntity : claimEntities) {
      String claimId = extractClaimId(claimEntity);
      claimIds.add(claimId);
    }
    return claimIds;
  }

  /**
   * extracts ClaimId.
   *
   * @param claimEntity the claim entity
   * @return claim Id
   */
  public String extractClaimId(Object claimEntity) {

    if (claimEntity != null) {
      return getClaimId(claimEntity);
    }
    return "";
  }

  /**
   * Get Claim Ids.
   *
   * @param entity the resource being processed
   * @return String claim Id
   */
  public String getClaimId(Object entity) {
    if (entity == null) {
      return null; // return null if the entity is null
    }

    return switch (entity) {
      case RdaMcsClaim rdaMcsClaim -> rdaMcsClaim.getIdrClmHdIcn();
      case RdaFissClaim rdaFissClaim -> rdaFissClaim.getClaimId();
      case CarrierClaim carrierClaim -> String.valueOf(carrierClaim.getClaimId());
      case DMEClaim dmeClaim -> String.valueOf(dmeClaim.getClaimId());
      case HHAClaim hhaClaim -> String.valueOf(hhaClaim.getClaimId());
      case HospiceClaim hospiceClaim -> String.valueOf(hospiceClaim.getClaimId());
      case InpatientClaim inpatientClaim -> String.valueOf(inpatientClaim.getClaimId());
      case OutpatientClaim outpatientClaim -> String.valueOf(outpatientClaim.getClaimId());
      case SNFClaim snfClaim -> String.valueOf(snfClaim.getClaimId());
      case PartDEvent partDEvent -> String.valueOf(partDEvent.getEventId());

      default -> null; // Return null for unsupported claim types
    };
  }
}
