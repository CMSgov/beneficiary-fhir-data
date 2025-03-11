package gov.cms.bfd.server.war.commons;

import jakarta.persistence.EntityManager;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Provides common logic for performing DB interactions. */
public class SecurityTagsDao {

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

      claimIdToTagsMap.computeIfAbsent(claimId, k -> new HashSet<>()).add(tag);
    }

    return claimIdToTagsMap;
  }
}
