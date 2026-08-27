package gov.cms.bfd.server.ng.claim.model.institutional;

import gov.cms.bfd.server.ng.claim.converter.FissPpsIndicatorCodeConverter;
import gov.cms.bfd.server.ng.claim.model.common.PpsIndicatorCode;
import gov.cms.bfd.server.ng.claim.model.common.SupportingInfoComponentBase;
import gov.cms.bfd.server.ng.claim.model.common.SupportingInfoFactory;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.Getter;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

/** The institutional Shared Systems CMS profile supporting info. */
@Embeddable
@Getter
public class ClaimInstitutionalCmsSharedSystemsSupportingInfo
    implements SupportingInfoComponentBase {

  @Column(name = "clm_pps_ind")
  @Convert(converter = FissPpsIndicatorCodeConverter.class)
  private Optional<PpsIndicatorCode> ppsIndicatorCode;

  @Embedded ClaimInstitutionalSupportingInfoBase claimInstitutionalSupportingInfo;

  @Override
  public List<ExplanationOfBenefit.SupportingInformationComponent> toFhir(
      SupportingInfoFactory supportingInfoFactory) {
    return Stream.concat(
            claimInstitutionalSupportingInfo.toFhir(supportingInfoFactory).stream(),
            Stream.of(ppsIndicatorCode.map(c -> c.toFhir(supportingInfoFactory)))
                .flatMap(Optional::stream))
        .toList();
  }
}
