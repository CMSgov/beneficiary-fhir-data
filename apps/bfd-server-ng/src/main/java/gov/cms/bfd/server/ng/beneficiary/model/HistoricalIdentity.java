package gov.cms.bfd.server.ng.beneficiary.model;

import gov.cms.bfd.server.ng.DateUtil;
import gov.cms.bfd.server.ng.SystemUrl;
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

@Entity
public class HistoricalIdentity {
  @Id Long id;
  Long beneSk;
  Optional<String> mbi;
  Optional<LocalDate> effectiveDate;
  Optional<LocalDate> obsoleteDate;

  public HistoricalIdentity(
      Long id,
      Long beneSk,
      String mbi,
      Optional<LocalDate> effectiveDate,
      Optional<LocalDate> obsoleteDate) {
    this.id = id;
    this.beneSk = beneSk;
    this.mbi = Optional.ofNullable(mbi);
    this.effectiveDate = effectiveDate;
    this.obsoleteDate = obsoleteDate;
  }

  public Optional<Identifier> toFhirIdentifier() {
    if (mbi.isEmpty()) {
      return Optional.empty();
    }
    var mbiId = new Identifier().setSystem(SystemUrl.CMS_MBI).setValue(mbi.get());
    effectiveDate.ifPresent(
        e -> {
          var period = new Period().setStart(DateUtil.toDate(e));
          obsoleteDate.ifPresent(o -> period.setEnd(DateUtil.toDate(e)));
          mbiId.setPeriod(period);
        });

    var mbiCoding =
        new CodeableConcept()
            .setCoding(List.of(new Coding().setSystem(SystemUrl.HL7_IDENTIFIER).setCode("MB")));
    mbiId.setType(mbiCoding);

    return Optional.of(mbiId);
  }

  public Optional<Patient.PatientLinkComponent> toFhirLink(Patient currentPatient) {
    var currentSk = currentPatient.getId();
    if (!beneSk.toString().equals(currentSk)) {
      var link = new Patient.PatientLinkComponent();
      link.setType(Patient.LinkType.REPLACES);
      link.setOther(new Reference(currentPatient));
      return Optional.of(link);
    } else {
      return Optional.empty();
    }
  }
}
