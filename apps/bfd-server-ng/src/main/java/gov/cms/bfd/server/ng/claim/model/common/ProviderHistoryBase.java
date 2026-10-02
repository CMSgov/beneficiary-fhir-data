package gov.cms.bfd.server.ng.claim.model.common;

import gov.cms.bfd.server.ng.util.SystemUrls;
import jakarta.persistence.MappedSuperclass;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;
import org.hl7.fhir.r4.model.Reference;

/** Provider History. */
@SuppressWarnings({"checkstyle:MissingJavadocMethod", "checkstyle:MissingJavadocType"})
@Getter
@MappedSuperclass
public abstract class ProviderHistoryBase {

  private Optional<String> providerNpiNumber;
  private Optional<String> providerName;

  private Optional<Integer> npiType;

  /** Represents the enum NPI Type. */
  @Getter
  @AllArgsConstructor
  public enum NpiType {
    /** NPI belongs to an individual. */
    INDIVIDUAL("Practitioner"),
    /** NPI belongs to an organization. */
    ORGANIZATION("Organization"),
    /** Unknown NPI Type. */
    UNKNOWN("");

    private final String type;

    public static NpiType fromNpiTypeCode(Optional<Integer> npiTypeCode) {
      if (npiTypeCode.isEmpty()) {
        return UNKNOWN;
      }
      if (npiTypeCode.get().equals(2)) {
        return ORGANIZATION;
      }
      return INDIVIDUAL;
    }
  }

  public abstract CareTeamType getCareTeamType(Optional<ClaimTypeCode> claimTypeCode);

  public NpiType getNpiType() {
    return NpiType.fromNpiTypeCode(npiType);
  }

  public Optional<ExplanationOfBenefit.CareTeamComponent> toFhirCareTeamComponent(
      Integer sequence, Optional<ClaimTypeCode> claimTypeCode) {
    if (providerNpiNumber.isEmpty()) {
      return Optional.empty();
    }
    var providerReference =
        ProviderFhirHelper.createProviderReference(providerNpiNumber.get(), providerName);

    var providerNpiType = getNpiType();
    if (providerNpiType != NpiType.UNKNOWN) {
      providerReference.setType(providerNpiType.getType());
    }

    return getCareTeamComponent(sequence, providerReference, claimTypeCode);
  }

  public Optional<ExplanationOfBenefit.CareTeamComponent> getCareTeamComponent(
      Integer sequence, Reference providerReference, Optional<ClaimTypeCode> claimTypeCode) {
    CodeableConcept roleConcept = new CodeableConcept();
    var role = getCareTeamType(claimTypeCode).getRoleCode();
    var display = getCareTeamType(claimTypeCode).getRoleDisplay();
    // Always add THO coding
    roleConcept.addCoding(
        new Coding()
            .setSystem(SystemUrls.HL7_THO_CLAIM_CARE_TEAM_ROLE)
            .setCode(role)
            .setDisplay(display));

    // Add legacy C4BB coding unless supervisor
    if (CareTeamType.SUPERVISOR != getCareTeamType(claimTypeCode)) {
      roleConcept.addCoding(
          new Coding()
              .setSystem(SystemUrls.CARIN_CODE_SYSTEM_CLAIM_CARE_TEAM_ROLE)
              .setCode(role)
              .setDisplay(display));
    }

    return Optional.of(
        new ExplanationOfBenefit.CareTeamComponent()
            .setSequence(sequence)
            .setRole(roleConcept)
            .setProvider(providerReference));
  }
}
