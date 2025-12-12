package gov.cms.bfd.server.ng.coverage.converter;

import gov.cms.bfd.server.ng.coverage.model.BeneficiaryLISCopaymentLevelCode;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Optional;

/**
 * Converts a beneficiary low income subsidy copayment level code to and from the database
 * representation.
 */
@Converter(autoApply = true)
public class BeneficiaryLISCopaymentLevelCodeConverter
    implements AttributeConverter<Optional<BeneficiaryLISCopaymentLevelCode>, String> {
  @Override
  public String convertToDatabaseColumn(Optional<BeneficiaryLISCopaymentLevelCode> code) {
    return code.map(BeneficiaryLISCopaymentLevelCode::getCode).orElse("");
  }

  @Override
  public Optional<BeneficiaryLISCopaymentLevelCode> convertToEntityAttribute(String code) {
    return BeneficiaryLISCopaymentLevelCode.tryFromCode(code);
  }
}
