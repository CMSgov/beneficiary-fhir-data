package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.util.SequenceGenerator;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@Embeddable
class CareTeam {
  @Column(name = "clm_atndg_prvdr_npi_num")
  private Optional<String> attendingProviderNpiNumber;

  // TODO: last names should be sourced from NPPES in the future
  @Column(name = "clm_atndg_prvdr_last_name")
  private Optional<String> attendingProviderLastName;

  @Column(name = "clm_oprtg_prvdr_npi_num")
  private Optional<String> operatingProviderNpiNumber;

  @Column(name = "clm_oprtg_prvdr_last_name")
  private Optional<String> operatingProviderLastName;

  @Column(name = "clm_othr_prvdr_npi_num")
  private Optional<String> otherProviderNpiNumber;

  @Column(name = "clm_othr_prvdr_last_name")
  private Optional<String> otherProviderLastName;

  @Column(name = "clm_rndrg_prvdr_npi_num")
  private Optional<String> renderingProviderNpiNumber;

  @Column(name = "clm_rndrg_prvdr_last_name")
  private Optional<String> renderingProviderLastName;

  @Column(name = "prvdr_rfrg_prvdr_npi_num")
  private Optional<String> refferingProviderNpiNumber;

  @Column(name = "clm_rfrg_prvdr_pin_num")
  private Optional<String> refferingProviderPinNumber;




    List<CareTeamType.CareTeamComponents> toFhir(int sequenceStart) {
    var sequenceGenerator = new SequenceGenerator(sequenceStart);
    var components =
        Stream.of(
            attendingProviderNpiNumber.map(
                npi ->
                    CareTeamType.ATTENDING.toFhir(
                        sequenceGenerator, npi, attendingProviderLastName,Optional.empty())),
            operatingProviderNpiNumber.map(
                npi ->
                    CareTeamType.OPERATING.toFhir(
                        sequenceGenerator, npi, operatingProviderLastName,Optional.empty())),
            otherProviderNpiNumber.map(
                npi ->
                    CareTeamType.RENDERING.toFhir(sequenceGenerator, npi, otherProviderLastName,Optional.empty())),
            renderingProviderNpiNumber.map(
                npi ->
                    CareTeamType.RENDERING.toFhir(
                        sequenceGenerator, npi, renderingProviderLastName,Optional.empty())),
            refferingProviderNpiNumber.map(
                npi ->
                        CareTeamType.REFERRING.toFhir(
                                sequenceGenerator, npi, renderingProviderLastName, refferingProviderPinNumber)));

    return components.flatMap(Optional::stream).toList();
  }
}
