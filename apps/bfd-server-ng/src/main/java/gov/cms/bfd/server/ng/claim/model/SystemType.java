package gov.cms.bfd.server.ng.claim.model;

import java.util.List;

/** The type of system for a given claim. Used to group various systems under SS together. */
public enum SystemType {
  /** National Claims History. */
  NCH,
  /** Shared Systems. */
  SS,
  /** Drug Data Processing systems. */
  DDPS;

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
    return switch (this) {
      case NCH -> source == MetaSourceSk.NCH;
      case DDPS -> source == MetaSourceSk.DDPS;
      case SS -> source != MetaSourceSk.NCH && source != MetaSourceSk.DDPS;
    };
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
    };
  }
}
