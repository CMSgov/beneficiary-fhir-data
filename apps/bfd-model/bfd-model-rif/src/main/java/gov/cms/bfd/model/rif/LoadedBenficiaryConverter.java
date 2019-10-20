package gov.cms.bfd.model.rif;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

/**
 * Hiberate converter for the beneficiaries list. Just a comma seperated list the for the db format.
 */
@Converter
public class LoadedBenficiaryConverter implements AttributeConverter<List<String>, String> {
  public static final String SEPARATOR = ",";

  @Override
  public String convertToDatabaseColumn(List<String> beneficiaries) {
    if (beneficiaries == null || beneficiaries.isEmpty()) {
      return "";
    }
    return beneficiaries.stream().collect(Collectors.joining(SEPARATOR));
  }

  @Override
  public List<String> convertToEntityAttribute(String beneficiaries) {
    if (beneficiaries == null || beneficiaries.isEmpty()) {
      return new ArrayList<>();
    }
    return Arrays.asList(beneficiaries.split(SEPARATOR, -1));
  }
}
