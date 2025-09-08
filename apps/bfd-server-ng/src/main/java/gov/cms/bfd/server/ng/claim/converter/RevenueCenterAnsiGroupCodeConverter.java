package gov.cms.bfd.server.ng.claim.converter;

import gov.cms.bfd.server.ng.claim.model.RevenueCenterAnsiGroupCode;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Optional;

/** Database code converter. */
@Converter(autoApply = true)
public class RevenueCenterAnsiGroupCodeConverter
        implements AttributeConverter<Optional<RevenueCenterAnsiGroupCode>, String> {
    @Override
    public String convertToDatabaseColumn(
            Optional<RevenueCenterAnsiGroupCode> revenueCenterAnsiGroupCode) {
        // This is a read-only API so this method will never actually persist anything to the database.
        return revenueCenterAnsiGroupCode.map(RevenueCenterAnsiGroupCode::getCode).orElse("");
    }

    @Override
    public Optional<RevenueCenterAnsiGroupCode> convertToEntityAttribute(String code) {
        return RevenueCenterAnsiGroupCode.tryFromCode(code);
    }
}
