package gov.cms.bfd.server.ng.claim.converter;

import gov.cms.bfd.server.ng.claim.model.ClaimLineHCTHGBTestTypeCode;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Optional;

/** Database code converter. */
@Converter(autoApply = true)
public class ClaimLineHCTHGBTestTypeCodeConverter
        implements AttributeConverter<Optional<ClaimLineHCTHGBTestTypeCode>, String> {
    @Override
    public String convertToDatabaseColumn(
            Optional<ClaimLineHCTHGBTestTypeCode> claimLineHCTHGBTestTypeCode) {
        // This is a read-only API so this method will never actually persist anything to the database.
        return claimLineHCTHGBTestTypeCode.map(ClaimLineHCTHGBTestTypeCode::getCode).orElse("");
    }

    @Override
    public Optional<ClaimLineHCTHGBTestTypeCode> convertToEntityAttribute(String code) {
        return ClaimLineHCTHGBTestTypeCode.tryFromIdrCode(code);
    }
}
