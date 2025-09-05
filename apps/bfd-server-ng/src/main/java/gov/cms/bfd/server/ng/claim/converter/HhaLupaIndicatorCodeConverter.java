package gov.cms.bfd.server.ng.claim.converter;

import gov.cms.bfd.server.ng.claim.model.HhaLupaIndicatorCode;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Optional;

/** Database code converter. */
@Converter(autoApply = true)
public class HhaLupaIndicatorCodeConverter implements AttributeConverter<Optional<HhaLupaIndicatorCode>, String> {
    @Override
    public String convertToDatabaseColumn(Optional<HhaLupaIndicatorCode> hhaLupaIndicatorCode) {
        // This is a read-only API so this method will never actually persist anything to the database.
        return hhaLupaIndicatorCode.map(HhaLupaIndicatorCode::getCode).orElse("");
    }

    @Override
    public Optional<HhaLupaIndicatorCode> convertToEntityAttribute(String code) {
        return HhaLupaIndicatorCode.tryFromCode(code);
    }
}