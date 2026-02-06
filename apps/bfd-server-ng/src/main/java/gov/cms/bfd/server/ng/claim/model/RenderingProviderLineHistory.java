package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.util.SystemUrls;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.Optional;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Reference;

/** Rendering Provider Line History. * */
@Embeddable
@AttributeOverride(
    name = "providerNpiNumber",
    column = @Column(name = "prvdr_rndrng_prvdr_npi_num"))
@AttributeOverride(name = "providerSk", column = @Column(name = "prvdr_rndrng_sk"))
@AttributeOverride(
    name = "providerTaxonomyCode",
    column = @Column(name = "prvdr_rndrng_txnmy_cmpst_cd"))
@AttributeOverride(name = "providerTypeCode", column = @Column(name = "prvdr_rndrng_type_cd"))
@AttributeOverride(name = "providerOscarNumber", column = @Column(name = "prvdr_rndrng_oscar_num"))
@AttributeOverride(name = "providerFirstName", column = @Column(name = "prvdr_rndrng_1st_name"))
@AttributeOverride(name = "providerMiddleName", column = @Column(name = "prvdr_rndrng_mdl_name"))
@AttributeOverride(name = "providerLastName", column = @Column(name = "prvdr_rndrng_last_name"))
@AttributeOverride(name = "providerName", column = @Column(name = "prvdr_rndrng_name"))
@AttributeOverride(name = "providerLegalName", column = @Column(name = "prvdr_rndrng_lgl_name"))
@AttributeOverride(name = "employerIdNumber", column = @Column(name = "prvdr_rndrng_emplr_id_num"))
public class RenderingProviderLineHistory extends ProviderHistoryBase {

  @Column(name = "clm_rndrg_prvdr_prtcptg_cd")
  private Optional<String> participatingIndicatorCode;

  @Column(name = "clm_rndrg_prvdr_type_cd")
  private Optional<String> typeCode;

  @Override
  protected CareTeamType getCareTeamType() {
    return CareTeamType.RENDERING;
  }

  Optional<CareTeamType.CareTeamComponents> toFhirCareTeamComponentLine(
      Optional<Integer> claimLineNum) {

    var providerNpiOpt = getProviderNpiNumber();
    if (claimLineNum.isEmpty() || providerNpiOpt.isEmpty()) {
      return Optional.empty();
    }

    var name = new HumanName();
    getProviderFirstName().ifPresent(name::addGiven);
    getProviderMiddleName().ifPresent(name::addGiven);
    getProviderLastName().ifPresent(name::setFamily);

    var practitioner =
        ProviderFhirHelper.createPractitioner(
            "careteam-provider-line-" + claimLineNum.get(), providerNpiOpt.get(), name);

    participatingIndicatorCode.ifPresent(
        s ->
            practitioner.addExtension(
                new Extension()
                    .setUrl(SystemUrls.BLUE_BUTTON_STRUCTURE_DEFINITION_PROVIDER_PARTICIPATING_CODE)
                    .setValue(
                        new Coding()
                            .setCode(s)
                            .setSystem(
                                SystemUrls.BLUE_BUTTON_CODE_SYSTEM_PROVIDER_PARTICIPATING_CODE))));
    typeCode.ifPresent(
        s ->
            practitioner.addExtension(
                new Extension()
                    .setUrl(SystemUrls.BLUE_BUTTON_STRUCTURE_DEFINITION_PROVIDER_TYPE_CODE)
                    .setValue(
                        new Coding()
                            .setCode(s)
                            .setSystem(SystemUrls.BLUE_BUTTON_CODE_SYSTEM_PROVIDER_TYPE_CODE))));

    var component =
        new ExplanationOfBenefit.CareTeamComponent()
            .setSequence(claimLineNum.get())
            .setRole(
                new CodeableConcept(
                    new Coding()
                        .setSystem(SystemUrls.CARIN_CODE_SYSTEM_CLAIM_CARE_TEAM_ROLE)
                        .setCode(CareTeamType.RENDERING.getRoleCode())
                        .setDisplay(CareTeamType.RENDERING.getRoleDisplay())))
            .setProvider(new Reference("#careteam-provider-line-" + claimLineNum.get()));

    return Optional.of(new CareTeamType.CareTeamComponents(practitioner, component));
  }
}
