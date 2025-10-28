package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.util.SystemUrls;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

import java.util.Arrays;
import java.util.Optional;

@AllArgsConstructor
@Getter
public enum ClaimLineBrandGenericCode {

    /** B - Brand */
    B("B", "Brand"),
    /**
     * G - Generic Null/Missing
     */
    G("G", "Generic Null/Missing");

    private final String code;
    private final String display;

    /**
     * Convert from a database code.
     *
     * @param code database code
     * @return genric brand indicator code
     */
    public static Optional<ClaimLineBrandGenericCode> tryFromCode(String code) {
        return Arrays.stream(values()).filter(v -> v.code.equals(code)).findFirst();
    }

    ExplanationOfBenefit.SupportingInformationComponent toFhir(
            SupportingInfoFactory supportingInfoFactory) {
        var supportingInfo = supportingInfoFactory.createSupportingInfo();
        supportingInfo.setCategory(CarinSupportingInfoCategory.BRAND_GENERIC_IND_CODE.toFhir());

        var codeableConcept =
                new CodeableConcept()
                        .addCoding(
                                new Coding()
                                        .setSystem(SystemUrls.HL7_GENERIC_BRAND_IND)
                                        .setCode(code)
                                        .setDisplay(display))
                        .addCoding(
                                new Coding()
                                        .setSystem(SystemUrls.BLUE_BUTTON_GENERIC_BRAND_IND)
                                        .setCode(code)
                                        .setDisplay(display));
        supportingInfo.setCode(codeableConcept);
        return supportingInfo;
    }
}
