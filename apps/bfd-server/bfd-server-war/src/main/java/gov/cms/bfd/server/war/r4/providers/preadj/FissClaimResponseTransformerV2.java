package gov.cms.bfd.server.war.r4.providers.preadj;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.newrelic.api.agent.Trace;
import gov.cms.bfd.model.codebook.data.CcwCodebookMissingVariable;
import gov.cms.bfd.model.rda.PreAdjFissClaim;
import gov.cms.bfd.model.rda.PreAdjFissPayer;
import gov.cms.bfd.server.war.commons.BBCodingSystems;
import gov.cms.bfd.server.war.commons.CCWUtils;
import gov.cms.bfd.server.war.commons.carin.C4BBIdentifierType;
import gov.cms.bfd.server.war.r4.providers.preadj.common.AbstractTransformerV2;
import gov.cms.bfd.sharedutils.exceptions.BadCodeMonkeyException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.hl7.fhir.r4.model.ClaimResponse;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DateType;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.codesystems.ClaimType;

/** Transforms FISS/MCS instances into FHIR {@link ClaimResponse} resources. */
public class FissClaimResponseTransformerV2 extends AbstractTransformerV2 {

  private static final String METRIC_NAME =
      MetricRegistry.name(FissClaimResponseTransformerV2.class.getSimpleName(), "transform");

  private static final Map<Character, ClaimResponse.RemittanceOutcome> STATUS_TO_OUTCOME =
      Map.of(
          ' ', ClaimResponse.RemittanceOutcome.QUEUED,
          'a', ClaimResponse.RemittanceOutcome.QUEUED,
          's', ClaimResponse.RemittanceOutcome.PARTIAL,
          'p', ClaimResponse.RemittanceOutcome.COMPLETE,
          'd', ClaimResponse.RemittanceOutcome.COMPLETE,
          'i', ClaimResponse.RemittanceOutcome.PARTIAL,
          'r', ClaimResponse.RemittanceOutcome.COMPLETE,
          't', ClaimResponse.RemittanceOutcome.PARTIAL,
          'm', ClaimResponse.RemittanceOutcome.PARTIAL);

  private FissClaimResponseTransformerV2() {}

  /**
   * @param metricRegistry the {@link MetricRegistry} to use
   * @param claimEntity the FISS {@link PreAdjFissClaim} to transform
   * @return a FHIR {@link ClaimResponse} resource that represents the specified claim
   */
  @Trace
  static ClaimResponse transform(MetricRegistry metricRegistry, Object claimEntity) {
    if (!(claimEntity instanceof PreAdjFissClaim)) {
      throw new BadCodeMonkeyException();
    }

    try (Timer.Context ignored = metricRegistry.timer(METRIC_NAME).time()) {
      return transformClaim((PreAdjFissClaim) claimEntity);
    }
  }

  /**
   * @param claimGroup the {@link PreAdjFissClaim} to transform
   * @return a FHIR {@link ClaimResponse} resource that represents the specified {@link
   *     PreAdjFissClaim}
   */
  private static ClaimResponse transformClaim(PreAdjFissClaim claimGroup) {
    ClaimResponse claim = new ClaimResponse();

    claim.setId("f-" + claimGroup.getDcn());
    claim.setContained(List.of(getContainedPatient(claimGroup)));
    claim.setExtension(getExtension(claimGroup));
    claim.setIdentifier(getIdentifier(claimGroup));
    claim.setStatus(ClaimResponse.ClaimResponseStatus.ACTIVE);
    claim.setOutcome(STATUS_TO_OUTCOME.get(Character.toLowerCase(claimGroup.getCurrStatus())));
    claim.setType(getType());
    claim.setUse(ClaimResponse.Use.CLAIM);
    claim.setInsurer(new Reference().setIdentifier(new Identifier().setValue("CMS")));
    claim.setPatient(new Reference("#patient"));
    claim.setRequest(new Reference(String.format("Claim/f-%s", claimGroup.getDcn())));

    claim.setMeta(new Meta().setLastUpdated(Date.from(claimGroup.getLastUpdated())));
    claim.setCreated(new Date());

    return claim;
  }

  private static Resource getContainedPatient(PreAdjFissClaim claimGroup) {
    Optional<PreAdjFissPayer> optional =
        claimGroup.getPayers().stream()
            .filter(p -> p.getPayerType() == PreAdjFissPayer.PayerType.BeneZ)
            .findFirst();

    Patient patient;

    if (optional.isPresent()) {
      PreAdjFissPayer benePayer = optional.get();

      patient =
          getContainedPatient(
              claimGroup.getMbi(),
              new PatientInfo(
                  benePayer.getBeneFirstName(),
                  benePayer.getBeneLastName(),
                  ifNotNull(benePayer.getBeneMidInit(), s -> s.charAt(0) + "."),
                  benePayer.getBeneDob(),
                  benePayer.getBeneSex()));
    } else {
      patient = getContainedPatient(claimGroup.getMbi(), null);
    }

    return patient;
  }

  private static List<Extension> getExtension(PreAdjFissClaim claimGroup) {
    List<Extension> extensions = new ArrayList<>();
    extensions.add(
        new Extension(BBCodingSystems.FISS.CURR_STATUS)
            .setValue(
                new Coding(
                    BBCodingSystems.FISS.CURR_STATUS, "" + claimGroup.getCurrStatus(), null)));

    if (claimGroup.getReceivedDate() != null) {
      extensions.add(
          new Extension(BBCodingSystems.FISS.RECD_DT_CYMD)
              .setValue(new DateType(localDateToDate(claimGroup.getReceivedDate()))));
    }

    if (claimGroup.getCurrTranDate() != null) {
      extensions.add(new Extension(BBCodingSystems.FISS.CURR_TRAN_DT_CYMD));
    }

    return List.copyOf(extensions);
  }

  private static List<Identifier> getIdentifier(PreAdjFissClaim claimGroup) {
    return List.of(
        new Identifier()
            .setType(
                new CodeableConcept(
                    new Coding(
                        C4BBIdentifierType.UC.getSystem(),
                        C4BBIdentifierType.UC.toCode(),
                        C4BBIdentifierType.UC.getDisplay())))
            .setSystem(
                CCWUtils.calculateVariableReferenceUrl(
                    CcwCodebookMissingVariable.FI_DOC_CLM_CNTL_NUM))
            .setValue(claimGroup.getDcn()));
  }

  private static CodeableConcept getType() {
    return new CodeableConcept()
        .setCoding(
            List.of(
                new Coding(
                    ClaimType.INSTITUTIONAL.getSystem(),
                    ClaimType.INSTITUTIONAL.toCode(),
                    ClaimType.INSTITUTIONAL.getDisplay())));
  }
}
