package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.SequenceGenerator;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

@Embeddable
public class CareTeam {
  @Column(name = "clm_atndg_prvdr_npi_num")
  private Optional<String> attendingProviderNpiNumber;

  @Column(name = "clm_oprtg_prvdr_npi_num")
  private Optional<String> operatingProviderNpiNumber;

  @Column(name = "clm_othr_prvdr_npi_num")
  private Optional<String> otherProviderNpiNumber;

  @Column(name = "clm_rndrg_prvdr_npi_num")
  private Optional<String> renderingProviderNpiNumber;

  List<CareTeamType.CareTeamComponents> toFhir(ExplanationOfBenefit eob) {
    var sequenceGenerator = new SequenceGenerator();
    var components =
        Stream.of(
            attendingProviderNpiNumber.map(
                npi -> CareTeamType.ATTENDING.toFhir(sequenceGenerator, eob, npi)),
            operatingProviderNpiNumber.map(
                npi -> CareTeamType.OPERATING.toFhir(sequenceGenerator, eob, npi)),
            otherProviderNpiNumber.map(
                npi -> CareTeamType.RENDERING.toFhir(sequenceGenerator, eob, npi)),
            renderingProviderNpiNumber.map(
                npi -> CareTeamType.RENDERING.toFhir(sequenceGenerator, eob, npi)));

    return components.flatMap(Optional::stream).toList();
  }
}
