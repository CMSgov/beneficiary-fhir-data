package gov.cms.bfd.server.ng.beneficiary.model;

import gov.cms.bfd.server.ng.DateUtil;
import gov.cms.bfd.server.ng.SystemUrl;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.Reference;

@AllArgsConstructor
@Entity
public class HistoricalIdentity {
  @Id Long beneSk;
  @Id String mbi;
  Optional<LocalDate> effectiveDate;
  Optional<LocalDate> obsoleteDate;
  boolean isCurrentMbi;

  public FhirObject toFhir(Patient currentPatient) {
    var mbiId = new Identifier().setSystem(SystemUrl.CMS_MBI);
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

    var returnValue = new FhirObject();
    returnValue.setLink(Optional.empty());
    returnValue.setMbi(mbiId);
    var currentSk = currentPatient.getId();
    if (!beneSk.toString().equals(currentSk)) {
      var link = new Patient.PatientLinkComponent();
      link.setType(Patient.LinkType.REPLACES);
      link.setOther(new Reference(currentPatient));
      returnValue.setLink(Optional.of(link));
    }

    return returnValue;
  }

  @Data
  public static class FhirObject {
    private Identifier mbi;
    private Optional<Patient.PatientLinkComponent> link;
  }
}
