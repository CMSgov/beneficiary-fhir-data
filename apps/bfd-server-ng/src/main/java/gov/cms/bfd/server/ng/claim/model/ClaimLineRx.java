package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.ClaimFilterOptions;
import gov.cms.bfd.server.ng.util.DateUtil;
import gov.cms.bfd.server.ng.util.FhirUtil;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import lombok.Getter;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.DateType;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;
import org.hl7.fhir.r4.model.Observation;

/** Claim line info. */
@Embeddable
@Getter
@SuppressWarnings("java:S2201")
public class ClaimLineRx implements ClaimLineBase {

  @Column(name = "clm_line_from_dt")
  private Optional<LocalDate> fromDate;

  @Embedded private ClaimLineNdc ndc;
  @Embedded private ClaimLineServiceUnitQuantity serviceUnitQuantity;
  @Embedded private ClaimLineAdjudicationChargeRx adjudicationCharge;
  @Embedded private ClaimLineRxSupportingInfo claimRxSupportingInfo;

  /** Default constructor for JPA. */
  public ClaimLineRx() {
    // Default constructor for JPA
  }

  /**
   * Parameterized constructor.
   *
   * @param fromDate from date
   * @param ndc ndc code
   * @param serviceUnitQuantity service unit quantity
   * @param adjudicationCharge adjudication charge
   * @param claimRxSupportingInfo claim Rx supporting info
   */
  public ClaimLineRx(
      Optional<LocalDate> fromDate,
      ClaimLineNdc ndc,
      ClaimLineServiceUnitQuantity serviceUnitQuantity,
      ClaimLineAdjudicationChargeRx adjudicationCharge,
      ClaimLineRxSupportingInfo claimRxSupportingInfo) {
    this.fromDate = fromDate != null ? fromDate : Optional.empty();
    this.ndc = ndc;
    this.serviceUnitQuantity = serviceUnitQuantity;
    this.adjudicationCharge = adjudicationCharge;
    this.claimRxSupportingInfo = claimRxSupportingInfo;
  }

  /**
   * Parameterized constructor without adjudication charge.
   *
   * @param fromDate from date
   * @param ndc ndc code
   * @param serviceUnitQuantity service unit quantity
   * @param claimRxSupportingInfo claim Rx supporting info
   */
  public ClaimLineRx(
      Optional<LocalDate> fromDate,
      ClaimLineNdc ndc,
      ClaimLineServiceUnitQuantity serviceUnitQuantity,
      ClaimLineRxSupportingInfo claimRxSupportingInfo) {
    this.fromDate = fromDate != null ? fromDate : Optional.empty();
    this.ndc = ndc;
    this.serviceUnitQuantity = serviceUnitQuantity;
    this.adjudicationCharge = null;
    this.claimRxSupportingInfo = claimRxSupportingInfo;
  }

  @Override
  public Optional<Observation> toFhirObservation(int bfdRowId) {
    return Optional.empty();
  }

  @Override
  public Optional<ExplanationOfBenefit.ItemComponent> toFhirItemComponent(
      ClaimFilterOptions options) {

    var line = new ExplanationOfBenefit.ItemComponent();
    line.setSequence(1);
    var productOrService = new CodeableConcept();
    var quantity = serviceUnitQuantity.toFhir();
    claimRxSupportingInfo.toFhirNdcCompound().ifPresent(productOrService::addCoding);

    if (productOrService.isEmpty()) {
      ndc.toFhirCoding().ifPresent(productOrService::addCoding);
      ndc.getQualifier().ifPresent(quantity::setUnit);
    }

    line.setProductOrService(FhirUtil.checkDataAbsent(productOrService));
    ndc.toFhirDetail().ifPresent(line::addDetail);
    line.setQuantity(quantity);

    fromDate.map(d -> line.setServiced(new DateType(DateUtil.toDate(d))));

    if (adjudicationCharge != null) {
      adjudicationCharge.toFhir().forEach(line::addAdjudication);
    }

    return Optional.of(line);
  }

  @Override
  public List<ExplanationOfBenefit.SupportingInformationComponent> toFhirSupportingInfo(
      SupportingInfoFactory supportingInfoFactory) {
    return List.of();
  }

  @Override
  public Optional<RenderingCareTeamLine> getClaimLineRenderingProvider() {
    return Optional.empty();
  }

  @Override
  public Optional<Integer> getClaimLineNumber() {
    return Optional.empty();
  }

  @Override
  public Optional<String> getClaimLineDiagnosisCode() {
    return Optional.empty();
  }
}
