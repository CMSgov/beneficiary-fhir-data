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
import gov.cms.bfd.server.war.r4.providers.pac.common.ResourceTypeV2;
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
   * @param claimType the claim type
   * @param resourceType the claim type
   * @return set of ClaimIds
   */
  public Set<String> collectClaimIds(
      List<Object> claimEntities, ClaimType claimType, ResourceTypeV2 resourceType) {
    Set<String> claimIds = new HashSet<>();
    for (Object claimEntity : claimEntities) {
      String claimId = extractClaimId(claimEntity, claimType, resourceType);
      if (!claimId.isEmpty()) {
        claimIds.add(claimId);
      }
    }
    return claimIds;
  }

  /**
   * extracts ClaimId.
   *
   * @param claimEntity the claim entity
   * @param claimType the entityIdAttribute
   * @param resourceType the entityIdAttribute
   * @return claim Id
   */
  public String extractClaimId(
      Object claimEntity, ClaimType claimType, ResourceTypeV2 resourceType) {

    if (claimEntity == null) {
      return "";
    }

    if (claimType != null) {
      return getClaimIdByClaimType(claimType, claimEntity);
    } else if (resourceType != null) {
      return getClaimIdByResourceType(resourceType, claimEntity);
    }
    return "";
  }

  /**
   * Gets the claim ID for the given entity based on the claim type.
   *
   * @param claim the claim entity (CarrierClaim, DMEClaim, etc.)
   * @param claimType the claim type
   * @return the claim ID
   */
  public String getClaimIdByClaimType(ClaimType claimType, Object claim) {

    return switch (claimType) {
      case CARRIER -> String.valueOf(((CarrierClaim) claim).getClaimId());
      case DME -> String.valueOf(((DMEClaim) claim).getClaimId());
      case HHA -> String.valueOf(((HHAClaim) claim).getClaimId());
      case HOSPICE -> String.valueOf(((HospiceClaim) claim).getClaimId());
      case INPATIENT -> String.valueOf(((InpatientClaim) claim).getClaimId());
      case OUTPATIENT -> String.valueOf(((OutpatientClaim) claim).getClaimId());
      case PDE -> String.valueOf(((PartDEvent) claim).getEventId());
      case SNF -> String.valueOf(((SNFClaim) claim).getClaimId());
      default -> throw new IllegalArgumentException("Unsupported claim type: " + this);
    };
  }

  /**
   * Gets the claim ID for the given entity based on the resource type.
   *
   * @param resourceType the resource type (e.g., ResourceTypeV2)
   * @param claim the claim
   * @return claim id
   */
  private String getClaimIdByResourceType(ResourceTypeV2 resourceType, Object claim) {

    return switch (resourceType.getTypeLabel()) {
      case "carrier" -> String.valueOf(((CarrierClaim) claim).getClaimId());
      case "dme" -> String.valueOf(((DMEClaim) claim).getClaimId());
      case "hha" -> String.valueOf(((HHAClaim) claim).getClaimId());
      case "hospice" -> String.valueOf(((HospiceClaim) claim).getClaimId());
      case "inpatient" -> String.valueOf(((InpatientClaim) claim).getClaimId());
      case "outpatient" -> String.valueOf(((OutpatientClaim) claim).getClaimId());
      case "mcs" -> String.valueOf(((RdaMcsClaim) claim).getIdrClmHdIcn());
      case "fiss" -> String.valueOf(((RdaFissClaim) claim).getClaimId());
      case "snfclaim" -> String.valueOf(((SNFClaim) claim).getClaimId());
      default -> throw new IllegalArgumentException("Unsupported claim type: " + resourceType);
    };
  }
}
