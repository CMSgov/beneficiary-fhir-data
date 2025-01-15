package gov.cms.bfd.server.war.commons;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.hl7.fhir.r4.model.Coding;
import org.springframework.stereotype.Service;

/** Find security level. */
@Service
public final class SecurityTagManager {

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
   * Query tags for a specific claim from a specific table.
   *
   * @param claimId the name of the variable
   * @param tagClass the name of the tag class
   * @return queryTagsForClaim
   */
  private Set<String> queryTagsForClaim(String claimId, Class<?> tagClass) {

    String sql = "SELECT t.code FROM " + tagClass.getSimpleName() + " t WHERE t.claim = :claim";

    Query query = entityManager.createQuery(sql);
    query.setParameter("claim", claimId);

    @SuppressWarnings("unchecked")
    List<String> resultList = query.getResultList();

    return new HashSet<>(resultList);
  }

  /**
   * Determines the security level based on the collected tags.
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
   * Determines the security level based on the collected tags.
   *
   * @param claimId value of claimId
   * @param tagClass value of tagClass
   * @return SecurityLevel
   */
  public List<Coding> getClaimSecurityLevel(String claimId, Class<?> tagClass) {

    // Query tags associated with the claim
    List<String> securityTags = queryTagsForClaim(claimId, tagClass).stream().toList();

    List<Coding> securityTagCoding = new ArrayList<>();

    // If no security tags are found, directly add the default "Normal" tag
    if (securityTags.isEmpty()) {
      Coding coding = new Coding();
      coding
          .setSystem("http://terminology.hl7.org/CodeSystem/v3-Confidentiality")
          .setCode("N")
          .setDisplay("Normal");
      securityTagCoding.add(coding);
    } else {
      // Check for each tag and set corresponding code and display
      for (String securityTag : securityTags) {
        Coding coding = new Coding();

        // Check each security tag and apply corresponding values
        switch (securityTag) {
          case "R":
            coding
                .setSystem("http://terminology.hl7.org/CodeSystem/v3-Confidentiality")
                .setCode("R")
                .setDisplay("Restricted");
            break;
          case "42CFRPart2":
            coding
                .setSystem("http://terminology.hl7.org/CodeSystem/v3-ActCode")
                .setCode("42CFRPart2")
                .setDisplay("42 CFR Part 2");
            break;

          default:
            coding
                .setSystem("http://terminology.hl7.org/CodeSystem/v3-Confidentiality")
                .setCode("N") // Default to 'Normal' if unrecognized
                .setDisplay("Normal");
        }

        securityTagCoding.add(coding);
      }
    }

    return securityTagCoding;
  }

  /**
   * Determines the security level based on the collected tags.
   *
   * @param claimId value of claimId
   * @param tagClass value of tagClass
   * @return SecurityLevel
   */
  public List<org.hl7.fhir.dstu3.model.Coding> getClaimSecurityLevelDstu3(
      String claimId, Class<?> tagClass) {

    List<org.hl7.fhir.dstu3.model.Coding> securityTagCoding = new ArrayList<>();
    List<Coding> coding = getClaimSecurityLevel(claimId, tagClass);
    for (Coding code : coding) {
      org.hl7.fhir.dstu3.model.Coding securityTag = new org.hl7.fhir.dstu3.model.Coding();
      securityTag
          .setSystem("http://terminology.hl7.org/CodeSystem/v3-Confidentiality")
          .setCode(code.getCode()) // Default to 'Normal' if unrecognized
          .setDisplay(code.getDisplay());

      securityTagCoding.add(securityTag);
    }
    return securityTagCoding;
  }
}
