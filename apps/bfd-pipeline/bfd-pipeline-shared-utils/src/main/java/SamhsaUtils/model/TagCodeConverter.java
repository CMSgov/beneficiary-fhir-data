package SamhsaUtils.model;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

@Converter(autoApply = true)
public class TagCodeConverter implements AttributeConverter<TagCode, String> {
  @Override
  public String convertToDatabaseColumn(TagCode attribute) {
    if (attribute == null) {
      return null;
    }
    switch (attribute) {
      case _42CFRPart2 -> {
        return "42CFRPart2";
      }
      case R -> {
        return "R";
      }
      default -> throw new IllegalArgumentException("Unknown enum value " + attribute);
    }
  }

  @Override
  public TagCode convertToEntityAttribute(String dbData) {
    if (dbData == null) {
      return null;
    }
    switch (dbData) {
      case "42CFRPart2" -> {
        return TagCode._42CFRPart2;
      }
      case "R" -> {
        return TagCode.R;
      }
      default -> throw new IllegalArgumentException("Unknown database value " + dbData);
    }
  }
}
