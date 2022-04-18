package gov.cms.bfd.server.war.r4.providers.preadj;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.newrelic.api.agent.Trace;
import gov.cms.bfd.model.rda.RdaMcsClaim;
import gov.cms.bfd.model.rda.RdaMcsDetail;
import gov.cms.bfd.model.rda.RdaMcsDiagnosisCode;
import gov.cms.bfd.server.war.commons.BBCodingSystems;
import gov.cms.bfd.server.war.commons.TransformerConstants;
import gov.cms.bfd.server.war.commons.carin.C4BBIdentifierType;
import gov.cms.bfd.server.war.commons.carin.C4BBOrganizationIdentifierType;
import gov.cms.bfd.server.war.r4.providers.preadj.common.AbstractTransformerV2;
import gov.cms.bfd.sharedutils.exceptions.BadCodeMonkeyException;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.apache.commons.lang3.ObjectUtils;
import org.hl7.fhir.r4.model.Claim;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.Money;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.PositiveIntType;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.codesystems.ClaimType;
import org.hl7.fhir.r4.model.codesystems.ProcessPriority;

/** Transforms FISS/MCS instances into FHIR {@link Claim} resources. */
public class McsClaimTransformerV2 extends AbstractTransformerV2 {

  private static final String METRIC_NAME =
      MetricRegistry.name(McsClaimTransformerV2.class.getSimpleName(), "transform");

  private static final List<String> CANCELED_STATUS_CODES = List.of("r", "z", "9");

  private McsClaimTransformerV2() {}

  /**
   * @param metricRegistry the {@link MetricRegistry} to use
   * @param claimEntity the MCS {@link RdaMcsClaim} to transform
   * @return a FHIR {@link Claim} resource that represents the specified claim
   */
  @Trace
  static Claim transform(MetricRegistry metricRegistry, Object claimEntity) {
    if (!(claimEntity instanceof RdaMcsClaim)) {
      throw new BadCodeMonkeyException();
    }

    try (Timer.Context ignored = metricRegistry.timer(METRIC_NAME).time()) {
      return transformClaim((RdaMcsClaim) claimEntity);
    }
  }

  /**
   * @param claimGroup the {@link RdaMcsClaim} to transform
   * @return a FHIR {@link Claim} resource that represents the specified {@link RdaMcsClaim}
   */
  private static Claim transformClaim(RdaMcsClaim claimGroup) {
    Claim claim = new Claim();

    claim.setId("m-" + claimGroup.getIdrClmHdIcn());
    claim.setContained(List.of(getContainedPatient(claimGroup), getContainedProvider(claimGroup)));
    claim.setExtension(getExtension(claimGroup));
    claim.setIdentifier(getIdentifier(claimGroup));
    if (claimGroup.getIdrStatusCode() != null) {
      claim.setStatus(
          CANCELED_STATUS_CODES.contains(claimGroup.getIdrStatusCode().toLowerCase())
              ? Claim.ClaimStatus.CANCELLED
              : Claim.ClaimStatus.ACTIVE);
    }
    claim.setType(getType());
    claim.setBillablePeriod(getBillablePeriod(claimGroup));
    claim.setUse(Claim.Use.CLAIM);
    claim.setPriority(getPriority());
    claim.setTotal(getTotal(claimGroup));
    claim.setProvider(new Reference("#provider-org"));
    claim.setPatient(new Reference("#patient"));
    claim.setDiagnosis(getDiagnosis(claimGroup));
    claim.setItem(getItems(claimGroup));

    claim.setCreated(new Date());
    claim.setMeta(new Meta().setLastUpdated(Date.from(claimGroup.getLastUpdated())));

    return claim;
  }

  private static Resource getContainedPatient(RdaMcsClaim claimGroup) {
    PatientInfo patientInfo =
        new PatientInfo(
            ifNotNull(claimGroup.getIdrBeneFirstInit(), s -> s + "."),
            ifNotNull(claimGroup.getIdrBeneLast_1_6(), s -> s.charAt(0) + "."),
            ifNotNull(claimGroup.getIdrBeneMidInit(), s -> s + "."),
            null, // MCS claims don't contain dob
            claimGroup.getIdrBeneSex());

    return getContainedPatient(claimGroup.getIdrClaimMbi(), patientInfo);
  }

  private static Resource getContainedProvider(RdaMcsClaim claimGroup) {
    Organization organization = new Organization();

    if (claimGroup.getIdrBillProvType() != null) {
      organization
          .getExtension()
          .add(
              new Extension(BBCodingSystems.MCS.BILL_PROV_TYPE)
                  .setValue(
                      new Coding(
                          BBCodingSystems.MCS.BILL_PROV_TYPE,
                          claimGroup.getIdrBillProvType(),
                          null)));
    }

    if (claimGroup.getIdrBillProvSpec() != null) {
      organization
          .getExtension()
          .add(
              new Extension(BBCodingSystems.MCS.BILL_PROV_SPEC)
                  .setValue(
                      new Coding(
                          BBCodingSystems.MCS.BILL_PROV_SPEC,
                          claimGroup.getIdrBillProvSpec(),
                          null)));
    }

    if (claimGroup.getIdrBillProvEin() != null) {
      organization
          .getIdentifier()
          .add(
              new Identifier()
                  .setType(
                      new CodeableConcept(
                          new Coding(
                              C4BBOrganizationIdentifierType.TAX.getSystem(),
                              C4BBOrganizationIdentifierType.TAX.toCode(),
                              C4BBOrganizationIdentifierType.TAX.getDisplay())))
                  .setSystem(BBCodingSystems.MCS.BILL_PROV_EIN)
                  .setValue(claimGroup.getIdrBillProvEin()));
    }

    if (claimGroup.getIdrBillProvNum() != null) {
      organization
          .getIdentifier()
          .add(
              new Identifier()
                  .setType(
                      new CodeableConcept(
                          new Coding(
                              C4BBIdentifierType.NPI.getSystem(),
                              C4BBIdentifierType.NPI.toCode(),
                              C4BBIdentifierType.NPI.getDisplay())))
                  .setSystem(TransformerConstants.CODING_NPI_US)
                  .setValue(claimGroup.getIdrBillProvNpi()));
    }

    organization.setId("provider-org");

    return organization;
  }

  private static List<Extension> getExtension(RdaMcsClaim claimGroup) {
    return claimGroup.getIdrClaimType() == null
        ? null
        : List.of(
            new Extension(BBCodingSystems.MCS.CLM_TYPE)
                .setValue(
                    new Coding(BBCodingSystems.MCS.CLM_TYPE, claimGroup.getIdrClaimType(), null)));
  }

  private static List<Identifier> getIdentifier(RdaMcsClaim claimGroup) {
    return claimGroup.getIdrClmHdIcn() == null
        ? null
        : List.of(
            new Identifier()
                .setType(
                    new CodeableConcept(
                        new Coding(
                            C4BBIdentifierType.UC.getSystem(),
                            C4BBIdentifierType.UC.toCode(),
                            C4BBIdentifierType.UC.getDisplay())))
                .setSystem(BBCodingSystems.CARR_CLM_CONTROL_NUM)
                .setValue(claimGroup.getIdrClmHdIcn()));
  }

  private static CodeableConcept getType() {
    return new CodeableConcept()
        .setCoding(
            List.of(
                new Coding(
                    ClaimType.PROFESSIONAL.getSystem(),
                    ClaimType.PROFESSIONAL.toCode(),
                    ClaimType.PROFESSIONAL.getDisplay())));
  }

  private static Period getBillablePeriod(RdaMcsClaim claimGroup) {
    return new Period()
        .setStart(localDateToDate(claimGroup.getIdrHdrFromDateOfSvc()))
        .setEnd(localDateToDate(claimGroup.getIdrHdrToDateOfSvc()));
  }

  private static CodeableConcept getPriority() {
    return new CodeableConcept(
        new Coding(
            ProcessPriority.NORMAL.getSystem(),
            ProcessPriority.NORMAL.toCode(),
            ProcessPriority.NORMAL.getDisplay()));
  }

  private static Money getTotal(RdaMcsClaim claimGroup) {
    Money total;

    if (claimGroup.getIdrTotBilledAmt() != null) {
      total = new Money();

      total.setValue(claimGroup.getIdrTotBilledAmt());
      total.setCurrency("USD");
    } else {
      total = null;
    }

    return total;
  }

  private static List<Claim.DiagnosisComponent> getDiagnosis(RdaMcsClaim claimGroup) {
    return ObjectUtils.defaultIfNull(claimGroup.getDiagCodes(), List.<RdaMcsDiagnosisCode>of())
        .stream()
        .map(
            diagCode -> {
              String icdVersion = diagCode.getIdrDiagIcdType().equals("0") ? "10" : "9-cm";

              return new Claim.DiagnosisComponent()
                  .setSequence(diagCode.getPriority() + 1)
                  .setDiagnosis(
                      new CodeableConcept()
                          .setCoding(
                              List.of(
                                  new Coding(
                                      "http://hl7.org/fhir/sid/icd-" + icdVersion,
                                      diagCode.getIdrDiagCode(),
                                      null))));
            })
        .sorted(Comparator.comparing(Claim.DiagnosisComponent::getSequence))
        .collect(Collectors.toList());
  }

  private static List<Claim.ItemComponent> getItems(RdaMcsClaim claimGroup) {
    return ObjectUtils.defaultIfNull(claimGroup.getDetails(), List.<RdaMcsDetail>of()).stream()
        .map(
            detail -> {
              Claim.ItemComponent item =
                  new Claim.ItemComponent()
                      .setSequence(detail.getPriority() + 1)
                      .setProductOrService(
                          new CodeableConcept(
                              new Coding(BBCodingSystems.HCPCS, detail.getIdrProcCode(), null)))
                      .setServiced(
                          new Period()
                              .setStart(localDateToDate(detail.getIdrDtlFromDate()))
                              .setEnd(localDateToDate(detail.getIdrDtlToDate())))
                      .setModifier(getModifiers(detail));

              // Set the DiagnosisSequence only if the detail Dx Code is not null and present in the
              // Dx table.
              Optional.ofNullable(detail.getIdrDtlPrimaryDiagCode())
                  .ifPresent(
                      detailDiagnosisCode -> {
                        Optional<RdaMcsDiagnosisCode> matchingCode =
                            claimGroup.getDiagCodes().stream()
                                .filter(
                                    diagnosisCode ->
                                        codesAreEqual(
                                            diagnosisCode.getIdrDiagCode(), detailDiagnosisCode))
                                .findFirst();

                        matchingCode.ifPresent(
                            diagnosisCode ->
                                item.setDiagnosisSequence(
                                    List.of(new PositiveIntType(diagnosisCode.getPriority() + 1))));
                      });

              return item;
            })
        .sorted(Comparator.comparing(Claim.ItemComponent::getSequence))
        .collect(Collectors.toList());
  }

  private static List<CodeableConcept> getModifiers(RdaMcsDetail detail) {
    List<Optional<String>> mods =
        List.of(
            Optional.ofNullable(detail.getIdrModOne()),
            Optional.ofNullable(detail.getIdrModTwo()),
            Optional.ofNullable(detail.getIdrModThree()),
            Optional.ofNullable(detail.getIdrModFour()));

    // OptionalGetWithoutIsPresent - IsPresent used in filter
    //noinspection OptionalGetWithoutIsPresent
    return IntStream.range(0, mods.size())
        .filter(i -> mods.get(i).isPresent())
        .mapToObj(
            index ->
                new CodeableConcept(
                    new Coding(BBCodingSystems.HCPCS, mods.get(index).get(), null)
                        .setVersion(String.valueOf(index + 1))))
        .collect(Collectors.toList());
  }
}
