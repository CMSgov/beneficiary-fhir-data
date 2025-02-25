package gov.cms.bfd.server.war.commons;

import static gov.cms.bfd.server.war.SpringConfiguration.SSM_PATH_SAMHSA_V2_ENABLED;

import gov.cms.bfd.server.war.r4.providers.pac.common.ClaimWithSecurityTagsV2;
import gov.cms.bfd.sharedutils.TagCode;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.hl7.fhir.r4.model.Coding;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** Find security level. */
@Service
public final class SecurityTagManager {

  /** The Entity manager. */
  private EntityManager entityManager;

  /** Flag to control whether SAMHSA filtering should be applied. */
  private final boolean samhsaV2Enabled;

  /**
   * Instantiates a new SecurityTagManager.
   *
   * @param samhsaV2Enabled samhsaV2Enabled flag
   */
  public SecurityTagManager(
      @Value("${" + SSM_PATH_SAMHSA_V2_ENABLED + ":false}") Boolean samhsaV2Enabled) {
    this.samhsaV2Enabled = samhsaV2Enabled;
  }

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
  public Set<String> queryTagsForClaim(String claimId, Class<?> tagClass) {

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
   * @param claimId value of claimId
   * @param tagClass value of tagClass
   * @return SecurityLevel
   */
  public List<Coding> getClaimSecurityLevel(String claimId, Class<?> tagClass) {

    if (samhsaV2Enabled) {
      // Query tags associated with the claim
      List<String> securityTags = queryTagsForClaim(claimId, tagClass).stream().toList();

      List<Coding> securityTagCoding = new ArrayList<>();

      // If no security tags are found, directly add the default "Normal" tag
      if (securityTags.isEmpty()) {
        Coding coding = new Coding();
        coding
            .setSystem(TransformerConstants.SAMHSA_CONFIDENTIALITY_CODE_SYSTEM_URL)
            .setCode("N")
            .setDisplay("Normal");
        securityTagCoding.add(coding);
      } else {
        // Check for each tag and set corresponding code and display
        for (String securityTag : securityTags) {
          Coding coding = new Coding();
          // Convert the securityTag string to the TagCode enum
          TagCode tagCode = TagCode.fromString(securityTag);
          // Check each security tag and apply corresponding values
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
      }
      return securityTagCoding;
    }
    return new ArrayList<>();
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
          .setSystem(TransformerConstants.SAMHSA_CONFIDENTIALITY_CODE_SYSTEM_URL)
          .setCode(code.getCode())
          .setDisplay(code.getDisplay());

      securityTagCoding.add(securityTag);
    }
    return securityTagCoding;
  }
}
