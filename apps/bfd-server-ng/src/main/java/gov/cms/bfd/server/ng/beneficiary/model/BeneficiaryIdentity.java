package gov.cms.bfd.server.ng.beneficiary.model;

import gov.cms.bfd.server.ng.DateUtil;
import gov.cms.bfd.server.ng.IdrConstants;
import gov.cms.bfd.server.ng.SystemUrls;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
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
  @Id
  @Column(name = "bene_sk")
  protected long beneSk;

  @Column(name = "bene_mbi_id")
  private Optional<String> mbi;

  @Column(name = "bene_xref_efctv_sk_computed")
  private long xrefSk;

  @Column(name = "bene_mbi_efctv_dt")
  private Optional<LocalDate> mbiEffectiveDate;

  @Column(name = "bene_mbi_obslt_dt")
  private Optional<LocalDate> mbiObsoleteDate;

  /**
   * Transforms the identity record to a FHIR {@link Identifier} if a valid MBI is present.
   *
   * @return identifier
   */
  public Optional<Identifier> toFhirIdentifier() {
    if (mbi.isEmpty()) {
      return Optional.empty();
    }
    var identifier = new Identifier().setSystem(SystemUrls.CMS_MBI).setValue(mbi.get());
    var period = new Period();
    mbiEffectiveDate.ifPresent(
        e -> {
          period.setStart(DateUtil.toDate(e));
        });
    mbiObsoleteDate.ifPresent(
        o -> {
          if (o.isBefore(IdrConstants.DEFAULT_DATE)) {
            period.setEnd(DateUtil.toDate(o));
          }
        });
    identifier.setPeriod(period);

    final var memberNumber = "MB";
    var mbiCoding =
        new CodeableConcept()
            .setCoding(
                List.of(new Coding().setSystem(SystemUrls.HL7_IDENTIFIER).setCode(memberNumber)));
    identifier.setType(mbiCoding);

    return Optional.of(identifier);
  }

  /**
   * Transforms the identity record to a {@link Patient.PatientLinkComponent} if the bene_sk is
   * different from the current beneficiary's bene_sk.
   *
   * @param requestedBeneSk bene_sk value for the beneficiary that was requested
   * @return patient link
   */
  public Optional<Patient.PatientLinkComponent> toFhirLink(long requestedBeneSk) {

    var beneSkMatches = beneSk == requestedBeneSk;
    var currentIsXref = xrefSk == beneSk;
    var requestedIsXref = xrefSk == requestedBeneSk;

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
    reference.setReference("Patient/" + beneSk);
    reference.setDisplay(String.valueOf(beneSk));
    link.setOther(reference);
    return link;
  }
}
