package gov.cms.bfd.server.ng.coverage.model;

import static gov.cms.bfd.server.ng.input.CoveragePart.DUAL;
import static gov.cms.bfd.server.ng.input.CoveragePart.PART_A;
import static gov.cms.bfd.server.ng.input.CoveragePart.PART_B;
import static gov.cms.bfd.server.ng.input.CoveragePart.PART_C;
import static gov.cms.bfd.server.ng.input.CoveragePart.PART_D;

import gov.cms.bfd.server.ng.input.CoveragePart;
import gov.cms.bfd.server.ng.model.ProfileType;
import gov.cms.bfd.server.ng.util.DateUtil;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Transient;
import java.time.ZonedDateTime;
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
   * Retrieves the updated timestamp for a given coverage part.
   *
   * @param coveragePart coverage Part
   * @return the updated timestamp
   */
  @Transient
  public ZonedDateTime getCoverageUpdateTimestamps(CoveragePart coveragePart) {
    return switch (coveragePart) {
      case PART_A -> partACoverageUpdatedTs;
      case PART_B -> partBCoverageUpdatedTs;
      case PART_C -> partCCoverageUpdatedTs;
      case PART_D -> partDCoverageUpdatedTs;
      case DUAL -> partDualCoverageUpdatedTs;
    };
  }

  /**
   * Builds Coverage meta using a supplied lastUpdated.
   *
   * @param coveragePart coverage Part
   * @param profileType FHIR profile URL to add
   * @return meta
   */
  public org.hl7.fhir.r4.model.Meta toFhir(ProfileType profileType, CoveragePart coveragePart) {
    var lastUpdated = getCoverageUpdateTimestamps(coveragePart);
    var meta = new org.hl7.fhir.r4.model.Meta().setLastUpdated(DateUtil.toDate(lastUpdated));
    profileType.getCoverageProfiles().forEach(meta::addProfile);
    return meta;
  }
}
