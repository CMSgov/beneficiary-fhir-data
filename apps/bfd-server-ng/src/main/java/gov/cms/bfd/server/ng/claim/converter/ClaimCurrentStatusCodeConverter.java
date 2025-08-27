package gov.cms.bfd.server.ng.claim.converter;

import gov.cms.bfd.server.ng.claim.model.ClaimCurrentStatusCode;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Optional;

/** Database code converter. */
@Converter(autoApply = true)
public class ClaimCurrentStatusCodeConverter
        implements AttributeConverter<Optional<ClaimCurrentStatusCode>, String> {
    @Override
    public String convertToDatabaseColumn(Optional<ClaimCurrentStatusCode> claimCurrentStatusCode) {
        // This is a read-only API so this method will never actually persist anything to the database.
        return claimCurrentStatusCode.map(ClaimCurrentStatusCode::getCode).orElse("");
    }

    @Override
    public Optional<ClaimCurrentStatusCode> convertToEntityAttribute(String idrCode) {
        return ClaimCurrentStatusCode.tryFromCode(idrCode);
    }
}