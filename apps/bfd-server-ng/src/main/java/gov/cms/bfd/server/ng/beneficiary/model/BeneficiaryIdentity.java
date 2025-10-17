package gov.cms.bfd.server.ng.beneficiary.model;

import gov.cms.bfd.server.ng.util.DateUtil;
import gov.cms.bfd.server.ng.util.SystemUrls;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.List;
import java.util.Optional;
import lombok.Getter;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.Reference;

/** The beneficiary_identity view, for getting current and historical bene_sks and MBIs. */
@Getter
@Table(name = "beneficiary_identity", schema = "idr")
@Entity
public class BeneficiaryIdentity {

  @EmbeddedId private BeneficiaryIdentityId id;

  /**
   * Transforms the identity record to a FHIR {@link Identifier} if a valid MBI is present.
   *
   * @return identifier
   */
  public Identifier toFhirIdentifier() {
    var identifier = new Identifier().setSystem(SystemUrls.CMS_MBI).setValue(id.getMbi());
    var period = new Period();
    id.getMbiEffectiveDate().ifPresent(e -> period.setStart(DateUtil.toDate(e)));
    id.getMbiObsoleteDate().ifPresent(o -> period.setEnd(DateUtil.toDate(o)));
    identifier.setPeriod(period);

    final var memberNumber = "MB";
    var mbiCoding =
        new CodeableConcept()
            .setCoding(
                List.of(new Coding().setSystem(SystemUrls.HL7_IDENTIFIER).setCode(memberNumber)));
    identifier.setType(mbiCoding);

    return identifier;
  }

  /**
   * Transforms the identity record to a {@link Patient.PatientLinkComponent} if the bene_sk is
   * different from the current beneficiary's bene_sk.
   *
   * @param requestedBeneSk bene_sk value for the beneficiary that was requested
   * @return patient link
   */
  public Optional<Patient.PatientLinkComponent> toFhirLink(long requestedBeneSk) {

    var beneSkMatches = id.getBeneSk() == requestedBeneSk;
    var currentIsXref = id.getXrefSk() == id.getBeneSk();
    var requestedIsXref = id.getXrefSk() == requestedBeneSk;

    // This identity record is the current xref record and it has a different bene_sk, so the
    // requested bene_sk is replaced by this one
    if (currentIsXref && !beneSkMatches) {
      return Optional.of(createLink(Patient.LinkType.REPLACEDBY));
    }

    // The requested bene_sk is the current xref record and it has a different bene_sk, so the
    // requested bene_sk replaces this one
    if (requestedIsXref && !beneSkMatches) {
      return Optional.of(createLink(Patient.LinkType.REPLACES));
    }

    return Optional.empty();
  }

  private Patient.PatientLinkComponent createLink(Patient.LinkType linkType) {
    var link = new Patient.PatientLinkComponent();
    link.setType(linkType);
    var reference = new Reference();
    reference.setReference("Patient/" + id.getBeneSk());
    reference.setDisplay(String.valueOf(id.getBeneSk()));
    link.setOther(reference);
    return link;
  }
}
