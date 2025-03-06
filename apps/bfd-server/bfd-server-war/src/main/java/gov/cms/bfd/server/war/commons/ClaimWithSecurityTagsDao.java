package gov.cms.bfd.server.war.commons;

import jakarta.persistence.EntityManager;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Provides common logic for performing DB interactions. */
public class ClaimWithSecurityTagsDao {

  private static final Logger LOGGER = LoggerFactory.getLogger(ClaimWithSecurityTagsDao.class);

  /**
   * Builds a mapping from claim IDs to their security tags.
   *
   * @param tagTable The table containing security tags
   * @param claimIds The list of claim IDs
   * @param entityManager the entityManager
   * @return A map from claim ID to a list of security tags
   */
  public Map<String, Set<String>> buildClaimIdToTagsMap(
      String tagTable, Set<String> claimIds, EntityManager entityManager) {
    // If no claim IDs, return an empty map
    if (claimIds.isEmpty()) {
      return Collections.emptyMap();
    }

    List<String[]> results = new ArrayList<>();

    if (tagTable != null) {
      String query =
          String.format("SELECT t.claim, t.code FROM %s t WHERE t.claim IN :claimIds", tagTable);

      results =
          entityManager
              .createQuery(query, String[].class)
              .setParameter("claimIds", claimIds)
              .getResultList();
    }

    // Build the map from results
    Map<String, Set<String>> claimIdToTagsMap = new HashMap<>();
    for (Object[] result : results) {
      String claimId = result[0].toString();
      String tag = result[1].toString();

      // Add tag to the list for this claim ID
      claimIdToTagsMap.computeIfAbsent(claimId, k -> new HashSet<>()).add(tag);
    }

    return claimIdToTagsMap;
  }

  /**
   * Builds a mapping from claim IDs to their security tags.
   *
   * @param claimEntity the Claim
   * @param claimType the claim type
   * @return A map from claim ID to a list of security tags
   */
  public String extractClaimId(Object claimEntity, ClaimType claimType) {
    try {
      // Dynamically access the field corresponding to the entityIdAttribute using reflection
      Field entityIdField =
          claimEntity.getClass().getDeclaredField(claimType.getEntityIdAttribute().getName());
      entityIdField.setAccessible(true); // Make the field accessible

      Object claimIdValue = entityIdField.get(claimEntity);

      if (claimIdValue != null) {
        return claimIdValue.toString();
      }
    } catch (NoSuchFieldException e) {
      // Field not found, try the next one
    } catch (IllegalAccessException e) {
      // Access error, try the next one
    }
    // If no ID found, return empty string or throw an exception
    return "";
  }

  /**
   * Builds a mapping from claim IDs to their security tags.
   *
   * @param claimEntities the claim entity
   * @param entityIdAttribute the claim type
   * @return set of ClaimIds
   */
  public Set<String> collectClaimIds(List<Object> claimEntities, String entityIdAttribute) {
    Set<String> claimIds = new HashSet<>();
    for (Object claimEntity : claimEntities) {

      try {
        // Dynamically access the field corresponding to the entityIdAttribute using reflection
        Field entityIdField = claimEntity.getClass().getDeclaredField(entityIdAttribute);
        entityIdField.setAccessible(true); // Make the field accessible

        // Get the value of the entityIdField
        Object claimIdValue = entityIdField.get(claimEntity);

        // If a valid claim ID is found, add it to the claimIds list
        if (claimIdValue != null) {
          claimIds.add(claimIdValue.toString());
        }
      } catch (NoSuchFieldException e) {
        LOGGER.debug("Field '{}' not found for claim entity: {}", entityIdAttribute, claimEntity);
      } catch (IllegalAccessException e) {
        LOGGER.error("Failed to access entity ID attribute for claim entity: {}", claimEntity, e);
      }
    }
    return claimIds;
  }

  /**
   * extracts ClaimId.
   *
   * @param claimEntity the claim entity
   * @param entityIdAttribute the entityIdAttribute
   * @return claim Id
   */
  public String extractClaimId(Object claimEntity, String entityIdAttribute) {
    try {
      // Dynamically access the field corresponding to the entityIdAttribute using reflection
      Field entityIdField = claimEntity.getClass().getDeclaredField(entityIdAttribute);
      entityIdField.setAccessible(true); // Make the field accessible

      Object claimIdValue = entityIdField.get(claimEntity);

      if (claimIdValue != null) {
        return claimIdValue.toString();
      }
    } catch (NoSuchFieldException e) {
      // Field not found, try the next one
    } catch (IllegalAccessException e) {
      // Access error, try the next one
    }
    // If no ID found, return empty string or throw an exception
    return "";
  }
}
