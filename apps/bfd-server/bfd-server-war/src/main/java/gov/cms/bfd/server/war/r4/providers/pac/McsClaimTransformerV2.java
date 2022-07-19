package gov.cms.bfd.server.war.r4.providers.pac;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.newrelic.api.agent.Trace;
import gov.cms.bfd.model.codebook.data.CcwCodebookMissingVariable;
import gov.cms.bfd.model.rda.RdaMcsClaim;
import gov.cms.bfd.model.rda.RdaMcsDetail;
import gov.cms.bfd.model.rda.RdaMcsDiagnosisCode;
import gov.cms.bfd.server.war.commons.BBCodingSystems;
import gov.cms.bfd.server.war.commons.CCWUtils;
import gov.cms.bfd.server.war.commons.TransformerConstants;
import gov.cms.bfd.server.war.commons.carin.C4BBIdentifierType;
import gov.cms.bfd.server.war.commons.carin.C4BBOrganizationIdentifierType;
import gov.cms.bfd.server.war.r4.providers.pac.common.AbstractTransformerV2;
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

  /**
   * Map used to calculate {@link Claim.ClaimStatus} from {@link RdaMcsClaim#getIdrStatusCode()} Any
   * item in this list is considered {@link Claim.ClaimStatus#CANCELLED}, every other value is
   * considered {@link Claim.ClaimStatus#ACTIVE}.
   */
  private static final List<String> CANCELED_STATUS_CODES = List.of("r", "z", "9");

  private McsClaimTransformerV2() {}

  /**
   * Transforms a given {@link RdaMcsClaim} into a FHIR {@link Claim} object, recording metrics with
   * the given {@link MetricRegistry}.
   *
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
   * Transforms a given {@link RdaMcsClaim} into a FHIR {@link Claim} object.
   *
   * @param claimGroup the {@link RdaMcsClaim} to transform
   * @return a FHIR {@link Claim} resource that represents the specified {@link RdaMcsClaim}
   */
  private static Claim transformClaim(RdaMcsClaim claimGroup) {
    Claim claim = new Claim();

    claim.setId("m-" + claimGroup.getIdrClmHdIcn());
    claim.setContained(List.of(getContainedPatient(claimGroup), getContainedProvider(claimGroup)));
    claim.setExtension(getExtension(claimGroup));
    claim.setIdentifier(getIdentifier(claimGroup));
    claim.setStatus(getStatus(claimGroup));
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

  /**
   * Parses out patient data from the given {@link RdaMcsClaim} object, creating a generic {@link
   * PatientInfo} object containing the patient data.
   *
   * @param claimGroup the {@link RdaMcsClaim} to parse.
   * @return The generated {@link PatientInfo} object with the parsed patient data.
   */
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

  /**
   * Parses out provider data from the given {@link RdaMcsClaim} object, creating a FHIR {@link
   * Organization} object containing the provider data.
   *
   * @param claimGroup the {@link RdaMcsClaim} to parse.
   * @return The generated {@link Organization} object with the parsed patient data.
   */
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

  /**
   * Parses out extension data from the given {@link RdaMcsClaim} object, creating a list of {@link
   * Extension} objects.
   *
   * @param claimGroup the {@link RdaMcsClaim} to parse.
   * @return The generated list of {@link Extension} objects with the parsed extension data.
   */
  private static List<Extension> getExtension(RdaMcsClaim claimGroup) {
    return claimGroup.getIdrClaimType() == null
        ? null
        : List.of(
            new Extension(BBCodingSystems.MCS.CLM_TYPE)
                .setValue(
                    new Coding(BBCodingSystems.MCS.CLM_TYPE, claimGroup.getIdrClaimType(), null)));
  }

  /**
   * Parses out identifier data from the given {@link RdaMcsClaim} object, creating a list of {@link
   * Identifier} objects.
   *
   * @param claimGroup the {@link RdaMcsClaim} to parse.
   * @return The generated list of {@link Identifier} objects with the parsed identifier data.
   */
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
                .setSystem(
                    CCWUtils.calculateVariableReferenceUrl(
                        CcwCodebookMissingVariable.CARR_CLM_CNTL_NUM))
                .setValue(claimGroup.getIdrClmHdIcn()));
  }

  /**
   * Parses out the claim status from the given {@link RdaMcsClaim} object, mapping it to a
   * corresponding {@link Claim.ClaimStatus} value.
   *
   * @param claimGroup the {@link RdaMcsClaim} to parse.
   * @return The {@link Claim.ClaimStatus} that corresponds to the given {@link
   *     RdaMcsClaim#getIdrStatusCode()}.
   */
  private static Claim.ClaimStatus getStatus(RdaMcsClaim claimGroup) {
    return claimGroup.getIdrStatusCode() == null
            || !CANCELED_STATUS_CODES.contains(claimGroup.getIdrStatusCode().toLowerCase())
        ? Claim.ClaimStatus.ACTIVE
        : Claim.ClaimStatus.CANCELLED;
  }

  /**
   * Creates a {@link CodeableConcept} containing the type data for this FHIR resource.
   *
   * @return A {@link CodeableConcept} object containing the type data.
   */
  private static CodeableConcept getType() {
    return new CodeableConcept()
        .setCoding(
            List.of(
                new Coding(
                    ClaimType.PROFESSIONAL.getSystem(),
                    ClaimType.PROFESSIONAL.toCode(),
                    ClaimType.PROFESSIONAL.getDisplay())));
  }

  /**
   * Parses out the billable period from the given {@link RdaMcsClaim} object, creating a FHIR
   * {@link Period} object.
   *
   * @param claimGroup the {@link RdaMcsClaim} to parse.
   * @return The {@link Period} object containing the billable period data.
   */
  private static Period getBillablePeriod(RdaMcsClaim claimGroup) {
    return new Period()
        .setStart(localDateToDate(claimGroup.getIdrHdrFromDateOfSvc()))
        .setEnd(localDateToDate(claimGroup.getIdrHdrToDateOfSvc()));
  }

  /**
   * Creates a {@link CodeableConcept} containing the priority data for this FHIR resource.
   *
   * @return A {@link CodeableConcept} object containing the priority data.
   */
  private static CodeableConcept getPriority() {
    return new CodeableConcept(
        new Coding(
            ProcessPriority.NORMAL.getSystem(),
            ProcessPriority.NORMAL.toCode(),
            ProcessPriority.NORMAL.getDisplay()));
  }

  /**
   * Parses out the claim total from the given {@link RdaMcsClaim} object, creating a FHIR {@link
   * Money} object.
   *
   * @param claimGroup the {@link RdaMcsClaim} to parse.
   * @return The {@link Money} object containing the claim total data.
   */
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

  /**
   * Parses out the diagnosis data from the given {@link RdaMcsClaim} object, creating a list of
   * FHIR {@link Claim.DiagnosisComponent} objects.
   *
   * @param claimGroup the {@link RdaMcsClaim} to parse.
   * @return The list of {@link Claim.DiagnosisComponent} objects containing the diagnosis data.
   */
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

  /**
   * Parses out the line item data from the given {@link RdaMcsClaim} object, creating a list of
   * FHIR {@link Claim.ItemComponent} objects.
   *
   * @param claimGroup the {@link RdaMcsClaim} to parse.
   * @return The list of {@link Claim.ItemComponent} objects containing the line item data.
   */
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

  /**
   * Parses out the modifier data from the given {@link RdaMcsDetail} object, creating a list of
   * FHIR {@link CodeableConcept} objects.
   *
   * @param detail the {@link RdaMcsDetail} to parse.
   * @return The list of {@link CodeableConcept} objects containing the modifier data.
   */
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
