package gov.cms.bfd.server.ng.patient;

import gov.cms.bfd.server.ng.DateUtil;
import gov.cms.bfd.server.ng.SystemUrls;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.Reference;

/**
 * A projection type created by joining the beneficiary, beneficiary_history, and
 * beneficiary_mbi_history tables.
 *
 * <p>This does not actually represent a table in the database, but we use the {@link Entity}
 * attribute here so it can be used in JPA queries and have converters applied to its fields.
 */
@Entity
public class PatientIdentity {
  // The id field is not actually used here, but JPA requires some unique ID for every entity, even
  // if it's just used for joins.
  // Ideally, we could use beneSk + mbi, but this becomes tricky because MBI may not always be
  // present.
  @Id Long rowId;
  String beneSk;
  String xrefSk;
  Optional<String> mbi;
  Optional<LocalDate> mbiEffectiveDate;
  Optional<LocalDate> mbiObsoleteDate;

  /**
   * Creates a new Identity record.
   *
   * @param rowId row ID from the query that created this object
   * @param beneSk bene_sk from the database
   * @param xrefSk bene_xref_sk from the database
   * @param mbi MBI from the database
   * @param mbiEffectiveDate MBI effective date
   * @param mbiObsoleteDate MBI obsolete date
   */
  public PatientIdentity(
      Long rowId,
      Long beneSk,
      Long xrefSk,
      String mbi,
      Optional<LocalDate> mbiEffectiveDate,
      Optional<LocalDate> mbiObsoleteDate) {
    this.rowId = rowId;
    this.beneSk = beneSk.toString();
    this.xrefSk = xrefSk.toString();
    this.mbi = Optional.ofNullable(mbi);
    this.mbiEffectiveDate = mbiEffectiveDate;
    this.mbiObsoleteDate = mbiObsoleteDate;
  }

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
    mbiEffectiveDate.ifPresent(
        e -> {
          var period = new Period().setStart(DateUtil.toDate(e));
          mbiObsoleteDate.ifPresent(o -> period.setEnd(DateUtil.toDate(o)));
          identifier.setPeriod(period);
        });

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
  public Optional<Patient.PatientLinkComponent> toFhirLink(String requestedBeneSk) {

    var beneSkMatches = beneSk.equals(requestedBeneSk);
    var currentIsXref = xrefSk.equals(beneSk);
    var requestedIsXref = xrefSk.equals(requestedBeneSk);

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
    reference.setDisplay(beneSk);
    link.setOther(reference);
    return link;
  }
}
