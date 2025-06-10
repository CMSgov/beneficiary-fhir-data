package gov.cms.bfd.server.ng.claim.converter;

import gov.cms.bfd.server.ng.claim.model.IcdIndicator;
import gov.cms.bfd.server.ng.claim.model.IdrUnit;
import jakarta.persistence.AttributeConverter;

import java.util.Optional;

public class IdrUnitConverter implements AttributeConverter<Optional<IdrUnit>, String> {
  @Override
  public String convertToDatabaseColumn(Optional<IdrUnit> unit) {
    // This is a read-only API so this method will never actually persist anything to the database.
    return unit.map(IdrUnit::getIdrCode).orElse("");
  }

  @Override
  public Optional<IdrUnit> convertToEntityAttribute(String code) {
    return IdrUnit.tryFromCode(code);
  }
}
