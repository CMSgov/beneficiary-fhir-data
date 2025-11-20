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

  @Column(name = "prvdr_prscrbng_prvdr_npi_num")
  private Optional<String> prescribingProviderNpiNumber;

  public static final String DEFAULT_PRESCRIBER_PROVIDER_LAST_NAME = "LAST NAME HERE";

  List<CareTeamType.CareTeamComponents> toFhir(
      Optional<ProviderHistory> attending,
      Optional<ProviderHistory> operating,
      Optional<ProviderHistory> other,
      Optional<ProviderHistory> rendering,
      Optional<ProviderHistory> prescribing) {
    var sequenceGenerator = new SequenceGenerator();
    var components =
        Stream.of(
            attendingProviderNpiNumber.map(
                npi ->
                    CareTeamType.ATTENDING.toFhir(
                        sequenceGenerator,
                        npi,
                        resolveLastName(attendingProviderLastName, attending))),
            operatingProviderNpiNumber.map(
                npi ->
                    CareTeamType.OPERATING.toFhir(
                        sequenceGenerator,
                        npi,
                        resolveLastName(operatingProviderLastName, operating))),
            otherProviderNpiNumber.map(
                npi ->
                    CareTeamType.OTHER.toFhir(
                        sequenceGenerator, npi, resolveLastName(otherProviderLastName, other))),
            renderingProviderNpiNumber.map(
                npi ->
                    CareTeamType.RENDERING.toFhir(
                        sequenceGenerator,
                        npi,
                        resolveLastName(renderingProviderLastName, rendering))),
            prescribingProviderNpiNumber.map(
                npi ->
                    CareTeamType.PRESCRIBING.toFhir(
                        sequenceGenerator,
                        npi,
                        resolveLastName(
                            Optional.of(DEFAULT_PRESCRIBER_PROVIDER_LAST_NAME), prescribing))));

    return components.flatMap(Optional::stream).toList();
  }

  private Optional<String> resolveLastName(
      Optional<String> legacyLastName, Optional<ProviderHistory> providerHistory) {
    return providerHistory.flatMap(ProviderHistory::getProviderLastName).or(() -> legacyLastName);
  }
}
