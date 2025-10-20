package gov.cms.bfd.server.ng.claim.converter;


import gov.cms.bfd.server.ng.claim.model.ProviderAssignmentIndicatorSwitch;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Optional;

/** Database code converter. */
@Converter(autoApply = true)
public class ProviderAssignmentIndicatorSwitchConverter
        implements AttributeConverter<Optional<ProviderAssignmentIndicatorSwitch>, String> {
    @Override
    public String convertToDatabaseColumn(
            Optional<ProviderAssignmentIndicatorSwitch> claimPaymentDenialCode) {
        // This is a read-only API so this method will never actually persist anything to the database.
        return claimPaymentDenialCode.map(ProviderAssignmentIndicatorSwitch::getCode).orElse("");
    }

    @Override
    public Optional<ProviderAssignmentIndicatorSwitch> convertToEntityAttribute(String code) {
        return ProviderAssignmentIndicatorSwitch.tryFromCode(code);
    }
}
