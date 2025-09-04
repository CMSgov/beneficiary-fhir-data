package gov.cms.bfd.server.ng.claim.model;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.hl7.fhir.r4.model.Extension;

@Embeddable
public class ClaimLineInstitutionalExtensions {
    @Embedded ClaimRevenueDiscountIndicatorCode claimRevenueDiscountIndicatorCode;
    @Embedded ClaimObligatedToAcceptasFinalOneIndicatorCode claimObligatedToAcceptasFinalOneIndicatorCode;
    @Embedded ClaimRevenuePackageIndicatorCode claimRevenuePackageIndicatorCode;
    @Embedded ClaimRevenuePaymentMethodCode claimRevenuePaymentMethodCode;
    @Embedded ClaimRevenueCenterStatusCode claimRevenueCenterStatusCode;

    List<Extension> toFhir() {
        return Stream.of(
                    claimRevenueDiscountIndicatorCode.toFhir(),
                    claimObligatedToAcceptasFinalOneIndicatorCode.toFhir(),
                    claimRevenuePackageIndicatorCode.toFhir(),
                    claimRevenuePaymentMethodCode.toFhir(),
                    claimRevenueCenterStatusCode.toFhir()
                )
                .flatMap(Optional::stream)
                .toList();
    }
}
