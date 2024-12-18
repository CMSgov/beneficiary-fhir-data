package gov.cms.bfd.server.war.commons;

import gov.cms.bfd.model.rda.samhsa.FissTag;
import gov.cms.bfd.model.rda.samhsa.McsTag;
import gov.cms.bfd.model.rif.samhsa.CarrierTag;
import gov.cms.bfd.model.rif.samhsa.DmeTag;
import gov.cms.bfd.model.rif.samhsa.HhaTag;
import gov.cms.bfd.model.rif.samhsa.HospiceTag;
import gov.cms.bfd.model.rif.samhsa.InpatientTag;
import gov.cms.bfd.model.rif.samhsa.OutpatientTag;
import gov.cms.bfd.model.rif.samhsa.SnfTag;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.springframework.stereotype.Service;

/** Find security level. */
@Service
public final class LookUpSamhsaSecurityTags {

  /** The Entity manager. */
  private EntityManager entityManager;

  /**
   * Sets the {@link #entityManager}.
   *
   * @param entityManager a JPA {@link EntityManager} connected to the application's database
   */
  @PersistenceContext
  public void setEntityManager(EntityManager entityManager) {
    this.entityManager = entityManager;
  }

  /**
   * Method to find the security level of a given claim ID.
   *
   * @param type holds Claim
   * @param claimId holds Claim
   * @return SecurityLevel
   */
  public String getClaimSecurityLevel(CodeableConcept type, String claimId) {
    // Identify the claim type (e.g., Inpatient, Outpatient, Carrier, etc.)

    // claimId = claim.getIdElement().getIdPart()
    //    CodeableConcept type, claimId

    // CodeableConcept type = claim.getType();
    String claimType = getClaimType(type);

    Set<String> securityTags = new HashSet<>();

    // Query tags based on the claim type
    switch (claimType) {
      case "Inpatient":
        securityTags.addAll(queryTagsForClaim(claimId, InpatientTag.class));
        break;
      case "Outpatient":
        securityTags.addAll(queryTagsForClaim(claimId, OutpatientTag.class));
        break;
      case "Carrier":
        securityTags.addAll(queryTagsForClaim(claimId, CarrierTag.class));
        break;
      case "DME":
        securityTags.addAll(queryTagsForClaim(claimId, DmeTag.class));
        break;
      case "HHA":
        securityTags.addAll(queryTagsForClaim(claimId, HhaTag.class));
        break;
      case "Hospice":
        securityTags.addAll(queryTagsForClaim(claimId, HospiceTag.class));
        break;
      case "SNF":
        securityTags.addAll(queryTagsForClaim(claimId, SnfTag.class));
        break;
      case "FISS":
        securityTags.addAll(queryTagsForClaim(claimId, FissTag.class));
        break;
      case "MCS":
        securityTags.addAll(queryTagsForClaim(claimId, McsTag.class));
        break;
      default:
        // No tags for unrecognized claim type
        break;
    }

    // Determine the security level based on the collected tags
    return determineSecurityLevel(securityTags);
  }

  /**
   * Helper method to get the claim type from the FHIR Claim resource.
   *
   * @param type the FHIR Claim resource
   * @return the claim type (e.g., "Inpatient", "Outpatient", etc.)
   */
  private String getClaimType(CodeableConcept type) {
    // Retrieve the claim type from the Claim resource
    //    CodeableConcept type = claim.getType();
    if (type != null && type.hasCoding()) {

      String code = type.getCodingFirstRep().getCode();
      switch (code) {
        case "INP":
          return "Inpatient";
        case "OUT":
          return "Outpatient";
        case "CAR":
          return "Carrier";
        case "DME":
          return "DME";
        case "HHA":
          return "HHA";
        case "HOS":
          return "Hospice";
        case "SNF":
          return "SNF";
        case "FISS":
          return "FISS";
        case "MCS":
          return "MCS";
        default:
          return "Unknown";
      }
    }
    return "Unknown"; // Return "Unknown" if type is not set
  }

  /**
   * Helper method to query tags for a specific claim from a specific table.
   *
   * @param claimId the name of the variable
   * @param tagClass the name of the tag class
   * @return queryTagsForClaim
   */
  private Set<String> queryTagsForClaim(String claimId, Class<?> tagClass) {
    Set<String> tagCodes = new HashSet<>();

    String sql;
    if (FissTag.class.equals(tagClass) || McsTag.class.equals(tagClass)) {
      // For FissTag and McsTag, claimId is a String
      sql = "SELECT t.code FROM " + tagClass.getSimpleName() + " t WHERE t.claim = :claim";
    } else {
      // For other tags (CarrierTag, DmeTag, etc.), claimId is a Long
      sql = "SELECT t.code FROM " + tagClass.getSimpleName() + " t WHERE t.claim = :claim";
    }

    Query query = entityManager.createQuery(sql);
    query.setParameter("claim", claimId);

    @SuppressWarnings("unchecked")
    List<String> resultList = query.getResultList();

    // Use a parameterized constructor to initialize the Set with the List
    tagCodes = new HashSet<>(resultList);
    return tagCodes;
  }

  /**
   * Helper method to determine the security level based on the collected tags.
   *
   * @param securityTags value of securityTags
   * @return SecurityLevel
   */
  private String determineSecurityLevel(Set<String> securityTags) {
    // Define the rules for security levels based on tag codes
    for (String tag : securityTags) {
      if ("R".equals(tag) || "42CFRPart2".equals(tag)) {
        return "Restricted"; // Sensitive data
      }
    }

    // Default to 'Normal' if no sensitive tags are found
    return "Normal";
  }

  /**
   * Helper method to determine the security level based on the collected tags.
   *
   * @param claimId value of claimId
   * @param tagClass value of tagClass
   * @return SecurityLevel
   */
  public String getClaimSecurityLevel(String claimId, Class<?> tagClass) {

    return determineSecurityLevel(queryTagsForClaim(claimId, tagClass));
    //    return queryTagsForClaim(claimId, tagClass);

  }
}
