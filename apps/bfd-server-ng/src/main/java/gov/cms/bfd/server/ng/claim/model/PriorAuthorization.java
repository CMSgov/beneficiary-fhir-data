package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.beneficiary.model.BeneficiarySimple;
import gov.cms.bfd.server.ng.util.DateUtil;
import gov.cms.bfd.server.ng.util.SequenceGenerator;
import gov.cms.bfd.server.ng.util.SystemUrls;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.SortedSet;
import java.util.UUID;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Reference;

/** Prior Authorization table. */
@Entity
@Getter
@Table(name = "prior_auth", schema = "idr")
@RequiredArgsConstructor
public class PriorAuthorization {

  @EmbeddedId private PriorAuthorizationId id;
  @Embedded private UniqueTrackingNumberPeriod uniqueTrackingNumberPeriod;
  @Embedded private OrderingOrReferringCareTeam orderingOrReferringProviderHistory;
  @Embedded private PriorAuthRenderingCareTeam renderingProviderHistory;
  @Embedded private AttendingPhysicianCareTeam attendingPhysicianProviderHistory;
  @Embedded private PriorAuthorizationMeta meta;
  @Embedded private PriorAuthorizationTypeOfBill typeOfBill;

  @Column(name = "resource_id")
  private UUID resourceId;

  @Column(name = "clm_type")
  private Optional<ClaimTypePriorAuth> claimType;

  @Column(name = "mac_id")
  private Optional<ClaimContractorNumber> macId;

  @Column(name = "icn_dcn")
  private String internalControlNumberOrDcn;

  @Column(name = "cms_cert")
  private String cmsCertificationNumber;

  @Column(name = "npi")
  private String facilityNpi;

  @Column(name = "name")
  private String facilityName;

  @OneToOne
  @JoinColumn(name = "mbi_num", referencedColumnName = "bene_mbi_id")
  private BeneficiarySimple beneficiary;

  @OneToMany(fetch = FetchType.EAGER)
  @JoinColumn(name = "mbi_num", referencedColumnName = "mbi_num")
  @JoinColumn(name = "utn", referencedColumnName = "utn")
  private SortedSet<PriorAuthorizationItem> items;

  private static final String INSURER_ORG = "insurer-org";

  @Transient protected SupportingInfoFactory supportingInfoFactory = new SupportingInfoFactory();

  MetaSourceSk getMetaSourceSk() {
    return MetaSourceSk.CWF;
  }

  /**
   * Convert the prior authorization info to a FHIR ExplanationOfBenefit.
   *
   * @param claimState the security status
   * @return ExplanationOfBenefit
   */
  public ExplanationOfBenefit toFhir(ClaimState claimState) {
    var eob = new ExplanationOfBenefit();
    eob.setId(resourceId.toString());
    eob.addIdentifier(
        new Identifier()
            .setSystem(SystemUrls.BLUE_BUTTON_UNIQUE_TRACKING_NUMBER)
            .setValue(id.getUniqueTrackingNumber()));
    eob.setStatus(ExplanationOfBenefit.ExplanationOfBenefitStatus.ACTIVE);
    eob.setUse(ExplanationOfBenefit.Use.PREAUTHORIZATION);
    eob.setPatient(PatientReferenceFactory.toFhir(beneficiary.getXrefSk()));
    eob.setMeta(meta.toFhir(getMetaSourceSk(), claimState.getSecurityStatus()));
    claimType.ifPresent(
        type -> {
          eob.setType(type.toFhir());
          eob.addInsurance(type.toFhirInsurance());
          if (type.equals(ClaimTypePriorAuth.Valid.B)) {
            eob.addIdentifier(
                new Identifier()
                    .setSystem(SystemUrls.BLUE_BUTTON_INTERNAL_CONTROL_NUMBER_OR_DCN)
                    .setValue(internalControlNumberOrDcn));
          }
        });
    eob.setPreAuthRefPeriod(List.of(uniqueTrackingNumberPeriod.toFhir()));
    addCareTeam(eob);
    eob.setProvider(new Reference("#" + getFacilityNpi()));
    eob.addContained(createOrganization());
    var insurer = toFhirInsurer();
    eob.addContained(insurer);
    eob.setInsurer(new Reference(insurer));
    var supportingInfo =
        Stream.of(
                macId.map(m -> m.toFhir(supportingInfoFactory)),
                Optional.of(typeOfBill.toFhir(supportingInfoFactory)))
            .flatMap(Optional::stream)
            .toList();
    supportingInfo.forEach(eob::addSupportingInfo);
    addItems(eob);

    return eob;
  }

  private void addCareTeam(ExplanationOfBenefit eob) {
    var sequenceGenerator = new SequenceGenerator();
    Stream.of(
            getOrderingOrReferringProviderHistory(),
            getRenderingProviderHistory(),
            getAttendingPhysicianProviderHistory())
        .flatMap(
            p -> p.toFhirCareTeamComponent(sequenceGenerator.next(), Optional.empty()).stream())
        .forEach(eob::addCareTeam);
  }

  private Organization createOrganization() {
    var org =
        ProviderFhirHelper.createOrganizationWithNpi(
            getFacilityNpi(), getFacilityNpi(), Optional.of(getFacilityName()));
    org.addIdentifier(
        new Identifier()
            .setSystem(SystemUrls.CMS_CERTIFICATION_NUMBERS)
            .setValue(getCmsCertificationNumber()));
    return org;
  }

  Organization toFhirInsurer() {
    var organization = OrganizationFactory.toFhir();
    organization.setId(INSURER_ORG);
    organization.setName("Centers for Medicare and Medicaid Services");
    return organization;
  }

  private void addItems(ExplanationOfBenefit eob) {
    if (getItems().isEmpty()) {
      return;
    }

    getItems().forEach(item -> item.toFhirItemComponent(claimType).ifPresent(eob::addItem));

    var outcome =
        getItems().stream()
                .map(PriorAuthorizationItem::getExtensions)
                .anyMatch(
                    ext ->
                        ext.getDecision()
                            .filter(d -> d == PriorAuthorizationDecision.Valid.P)
                            .isPresent())
            ? ExplanationOfBenefit.RemittanceOutcome.PARTIAL
            : ExplanationOfBenefit.RemittanceOutcome.COMPLETE;

    eob.setOutcome(outcome);

    getItems().stream()
        .map(item -> item.getExtensions().getPriorAuthDateUpdated())
        .flatMap(Optional::stream)
        .max(Comparator.naturalOrder())
        .map(DateUtil::toDate)
        .ifPresent(eob::setCreated);
  }
}
