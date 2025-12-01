package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.util.SequenceGenerator;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.hl7.fhir.r4.model.HumanName;

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

  @Column(name = "prvdr_rfrg_prvdr_npi_num")
  private Optional<String> referringProviderNpiNumber;

  @Column(name = "clm_rfrg_prvdr_pin_num")
  private Optional<String> referringProviderPinNumber;

  List<CareTeamType.CareTeamComponents> toFhir(
      Optional<ProviderHistory> attending,
      Optional<ProviderHistory> operating,
      Optional<ProviderHistory> other,
      Optional<ProviderHistory> rendering,
      Optional<ProviderHistory> prescribing,
      Optional<ProviderHistory> referring) {
    var sequenceGenerator = new SequenceGenerator();
    var components =
        Stream.of(
            attendingProviderNpiNumber.map(
                npi ->
                    CareTeamType.ATTENDING.toFhir(
                        sequenceGenerator,
                        npi,
                        resolveName(attending, attendingProviderLastName),
                        Optional.empty())),
            operatingProviderNpiNumber.map(
                npi ->
                    CareTeamType.OPERATING.toFhir(
                        sequenceGenerator,
                        npi,
                        resolveName(operating, operatingProviderLastName),
                        Optional.empty())),
            otherProviderNpiNumber.map(
                npi ->
                    CareTeamType.OTHER.toFhir(
                        sequenceGenerator,
                        npi,
                        resolveName(other, otherProviderLastName),
                        Optional.empty())),
            renderingProviderNpiNumber.map(
                npi ->
                    CareTeamType.RENDERING.toFhir(
                        sequenceGenerator,
                        npi,
                        resolveName(rendering, renderingProviderLastName),
                        Optional.empty())),
            prescribingProviderNpiNumber.map(
                npi ->
                    CareTeamType.PRESCRIBING.toFhir(
                        sequenceGenerator,
                        npi,
                        resolveName(prescribing, Optional.empty()),
                        Optional.empty())),
            referringProviderNpiNumber.map(
                npi ->
                    CareTeamType.REFERRING.toFhir(
                        sequenceGenerator,
                        npi,
                        resolveName(referring, Optional.empty()),
                        referringProviderPinNumber)));

    return components.flatMap(Optional::stream).toList();
  }

  private HumanName resolveName(
      Optional<ProviderHistory> provider, Optional<String> legacyLastName) {
    return provider.map(a -> a.toFhirName(legacyLastName)).orElse(new HumanName());
  }
}
