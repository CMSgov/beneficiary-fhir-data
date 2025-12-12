package gov.cms.bfd.server.ng.coverage.model;

import gov.cms.bfd.server.ng.input.CoveragePart;
import gov.cms.bfd.server.ng.model.ProfileType;
import gov.cms.bfd.server.ng.util.DateUtil;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Transient;
import java.time.ZonedDateTime;
import java.util.EnumMap;
import java.util.Map;
import lombok.Getter;

/** FHIR metadata information. */
@Embeddable
@Getter
public class Meta {
  @Column(name = "bfd_part_a_coverage_updated_ts", nullable = false)
  private ZonedDateTime partACoverageUpdatedTs;

  @Column(name = "bfd_part_b_coverage_updated_ts", nullable = false)
  private ZonedDateTime partBCoverageUpdatedTs;

  @Column(name = "bfd_part_c_coverage_updated_ts", nullable = false)
  private ZonedDateTime partCCoverageUpdatedTs;

  @Column(name = "bfd_part_d_coverage_updated_ts", nullable = false)
  private ZonedDateTime partDCoverageUpdatedTs;

  @Column(name = "bfd_part_dual_coverage_updated_ts", nullable = false)
  private ZonedDateTime partDualCoverageUpdatedTs;

  /**
   * Map holding all coverage updated timestamps.
   *
   * @return a CoveragePart updated timestamps map
   */
  @Transient
  public Map<CoveragePart, ZonedDateTime> getCoverageUpdateTimestamps() {
    var map = new EnumMap<CoveragePart, ZonedDateTime>(CoveragePart.class);
    map.put(CoveragePart.PART_A, partACoverageUpdatedTs);
    map.put(CoveragePart.PART_B, partBCoverageUpdatedTs);
    map.put(CoveragePart.DUAL, partDualCoverageUpdatedTs);
    return map;
  }

  /**
   * Builds Coverage meta using a supplied lastUpdated.
   *
   * @param coveragePart coverage Part
   * @param profileType FHIR profile URL to add
   * @return meta
   */
  public org.hl7.fhir.r4.model.Meta toFhir(ProfileType profileType, CoveragePart coveragePart) {
    var lastUpdated = getCoverageUpdateTimestamps().get(coveragePart);
    var meta = new org.hl7.fhir.r4.model.Meta().setLastUpdated(DateUtil.toDate(lastUpdated));
    profileType.getCoverageProfiles().forEach(meta::addProfile);
    return meta;
  }
}
