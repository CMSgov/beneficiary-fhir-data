package gov.cms.bfd.server.ng.claim.model;

import java.util.List;
import lombok.AllArgsConstructor;

/** The type of system for a given claim. Used to group various systems under SS together. */
@AllArgsConstructor
public enum SystemType {
  /** National Claims History. */
  NCH(List.of(MetaSourceSk.NCH)),
  /** Shared Systems. */
  SS(List.of(MetaSourceSk.FISS, MetaSourceSk.MCS, MetaSourceSk.VMS, MetaSourceSk.CWF)),
  /** Drug Data Processing systems. */
  DDPS(List.of(MetaSourceSk.DDPS)),
  /** Unknown (used for bene data). */
  UNKNOWN(List.of());

  private final List<MetaSourceSk> compatibleSources;

  /**
   * Checks if the system type is compatible with any of the given meta_src_sk values.
   *
   * @param sources meta_src_sk values to check
   * @return boolean
   */
  public boolean isCompatibleWithAny(List<MetaSourceSk> sources) {
    return sources.stream().anyMatch(this::isCompatibleWith);
  }

  /**
   * Checks if the system type is compatible with the given meta_src_sk.
   *
   * @param source meta_src_sk
   * @return boolean
   */
  public boolean isCompatibleWith(MetaSourceSk source) {
    return compatibleSources.contains(source);
  }

  /**
   * Whether the system type should filter out non-latest claims.
   *
   * @return boolean
   */
  public boolean filterLatestClaims() {
    return this != DDPS;
  }

  /**
   * Checks if the system type is compatible with claim source id.
   *
   * @param sourceId claim_src_id
   * @return boolean
   */
  public boolean isCompatibleWith(ClaimSourceId sourceId) {
    return switch (this) {
      case NCH, DDPS -> sourceId == ClaimSourceId.NATIONAL_CLAIMS_HISTORY;
      case SS -> sourceId != ClaimSourceId.NATIONAL_CLAIMS_HISTORY;
      case UNKNOWN -> false;
    };
  }
}
