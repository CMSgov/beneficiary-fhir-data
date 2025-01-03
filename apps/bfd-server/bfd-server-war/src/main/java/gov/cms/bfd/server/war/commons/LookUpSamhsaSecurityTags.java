package gov.cms.bfd.server.war.commons;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
  public String getClaimSecurityLevel(String claimId, Class<?> tagClass) {

    return determineSecurityLevel(queryTagsForClaim(claimId, tagClass));
  }
}
