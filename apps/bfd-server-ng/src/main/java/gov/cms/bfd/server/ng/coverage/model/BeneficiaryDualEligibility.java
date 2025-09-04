package gov.cms.bfd.server.ng.coverage.model;

import gov.cms.bfd.server.ng.DateUtil;
import gov.cms.bfd.server.ng.SystemUrls;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Coverage;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Period;

/** Dual Medicare/Medicaid eligibility info. */
@Entity
@Table(name = "beneficiary_dual_eligibility_latest", schema = "idr")
public class BeneficiaryDualEligibility {
  @Id
  @Column(name = "bene_sk")
  private long beneSk;

  @Column(name = "bene_dual_stus_cd")
  private String dualStatusCode;

  @Column(name = "bene_mdcd_elgblty_bgn_dt")
  private LocalDate eligibilityBeginDate;

  @Column(name = "bene_mdcd_elgblty_end_dt")
  private Optional<LocalDate> eligibilityEndDate;

  @Column(name = "bene_dual_type_cd")
  private String dualTypeCode;

  @Column(name = "geo_usps_state_cd")
  private String stateCode;

  Period toFhirPeriod() {
    var period = new Period().setStartElement(DateUtil.toFhirDate(eligibilityBeginDate));
    eligibilityEndDate.ifPresent(d -> period.setEndElement(DateUtil.toFhirDate(d)));
    return period;
  }

  Coverage.CoverageStatus toFhirStatus() {
    if (eligibilityEndDate.isPresent() && eligibilityEndDate.get().isBefore(DateUtil.nowAoe())) {
      return Coverage.CoverageStatus.CANCELLED;
    }

    return Coverage.CoverageStatus.ACTIVE;
  }

  List<Extension> toFhirExtensions() {
    var statusCodeExtension =
        new Extension()
            .setUrl(SystemUrls.BLUE_BUTTON_STRUCTURE_DEFINITION_DUAL_STATUS_CODE)
            .setValue(
                new Coding(
                    SystemUrls.BLUE_BUTTON_CODE_SYSTEM_DUAL_STATUS_CODE, dualStatusCode, null));

    var typeCodeExtension =
        new Extension()
            .setUrl(SystemUrls.BLUE_BUTTON_STRUCTURE_DEFINITION_DUAL_TYPE_CODE)
            .setValue(
                new Coding(SystemUrls.BLUE_BUTTON_CODE_SYSTEM_DUAL_TYPE_CODE, dualTypeCode, null));

    var stateCodeExtension =
        new Extension()
            .setUrl(SystemUrls.BLUE_BUTTON_STRUCTURE_DEFINITION_MEDICAID_STATE_CODE)
            .setValue(new Coding(SystemUrls.USPS, stateCode, null));

    return List.of(statusCodeExtension, typeCodeExtension, stateCodeExtension);
  }
}
