package gov.cms.model.rda.codegen.plugin.model;

import com.google.common.base.Strings;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ColumnBean {
  private static final Pattern NumericTypeRegex =
      Pattern.compile("(numeric|decimal)\\((\\d+)(,(\\d+))?\\)");
  private static final int NumericPrecisionGroup = 2;
  private static final int NumericScaleGroup = 4;

  private String name;
  private String dbName;
  private String sqlType;
  private String javaType;
  private String javaAccessorType;
  private String enumType;
  private String comment;
  private boolean nullable = true;
  private boolean identity = false;
  private boolean updatable = true;
  private FieldType fieldType = FieldType.Column;
  private SequenceBean sequence;

  public enum FieldType {
    Column,
    Transient
  }

  public String getColumnName() {
    return Strings.isNullOrEmpty(dbName) ? name : dbName;
  }

  public int computeLength() {
    Matcher matcher = Pattern.compile("char\\((\\d+)\\)").matcher(sqlType);
    if (matcher.find()) {
      return Integer.parseInt(matcher.group(1));
    } else if (sqlType.equalsIgnoreCase("varchar(max)")) {
      return Integer.MAX_VALUE;
    } else {
      return 0;
    }
  }

  public TypeName computeJavaType() {
    if (Strings.isNullOrEmpty(javaType)) {
      return mapSqlTypeToTypeName();
    } else if (isEnum()) {
      return ClassName.get(String.class);
    } else {
      return mapJavaTypeToTypeName(javaType);
    }
  }

  public TypeName computeJavaAccessorType() {
    if (Strings.isNullOrEmpty(javaAccessorType)) {
      return computeJavaType();
    } else {
      return mapJavaTypeToTypeName(javaAccessorType);
    }
  }

  public boolean hasDifferentAccessorType() {
    return !Strings.isNullOrEmpty(javaAccessorType);
  }

  public boolean hasComment() {
    return !Strings.isNullOrEmpty(comment);
  }

  public boolean isColumnDefRequired() {
    return sqlType.contains("decimal") || sqlType.contains("numeric");
  }

  public int getPrecision() {
    var matcher = NumericTypeRegex.matcher(sqlType);
    if (matcher.find()) {
      return Integer.parseInt(matcher.group(NumericPrecisionGroup));
    }
    return 0;
  }

  public int getScale() {
    var matcher = NumericTypeRegex.matcher(sqlType);
    if (matcher.find()) {
      var value = matcher.group(NumericScaleGroup);
      if (!Strings.isNullOrEmpty(value)) {
        return Integer.parseInt(value);
      }
    }
    return 0;
  }

  public boolean isEnum() {
    return !Strings.isNullOrEmpty(enumType);
  }

  public boolean isString() {
    return isStringType(computeJavaType()) && isStringType(mapSqlTypeToTypeName());
  }

  public boolean isChar() {
    return "char".equals(javaType);
  }

  public boolean isCharacter() {
    return "Character".equals(javaType);
  }

  public boolean isInt() {
    return "int".equals(javaType) || isIntType(mapSqlTypeToTypeName());
  }

  public boolean isDecimal() {
    return sqlType.contains("decimal") || sqlType.contains("numeric");
  }

  public boolean isDate() {
    return sqlType.contains("date");
  }

  public boolean hasSequence() {
    return sequence != null;
  }

  private boolean isStringType(TypeName type) {
    return (type instanceof ClassName) && ((ClassName) type).simpleName().equals("String");
  }

  private boolean isIntType(TypeName type) {
    return (type instanceof ClassName) && ((ClassName) type).simpleName().equals("Integer");
  }

  private TypeName mapSqlTypeToTypeName() {
    if (Strings.isNullOrEmpty(sqlType)) {
      throw new RuntimeException("no sqlType for column " + name);
    }
    if (sqlType.contains("char")) {
      return ClassName.get(String.class);
    }
    if (sqlType.contains("smallint")) {
      return ClassName.get(Short.class);
    }
    if (sqlType.equals("bigint")) {
      return ClassName.get(Long.class);
    }
    if (sqlType.equals("int")) {
      return ClassName.get(Integer.class);
    }
    if (sqlType.contains("decimal") || sqlType.contains("numeric")) {
      return ClassName.get(BigDecimal.class);
    }
    if (sqlType.contains("date")) {
      return ClassName.get(LocalDate.class);
    }
    if (sqlType.contains("timestamp")) {
      return ClassName.get(Instant.class);
    }
    throw new RuntimeException("no mapping for sqlType " + sqlType);
  }

  @SneakyThrows(ClassNotFoundException.class)
  private static TypeName mapJavaTypeToTypeName(String javaType) {
    switch (javaType) {
      case "char":
        return TypeName.CHAR;
      case "Character":
        return ClassName.get(Character.class);
      case "int":
        return TypeName.INT;
      case "Integer":
        return ClassName.get(Integer.class);
      case "short":
        return TypeName.SHORT;
      case "Short":
        return ClassName.get(Short.class);
      case "long":
        return TypeName.LONG;
      case "Long":
        return ClassName.get(Long.class);
      case "String":
        return ClassName.get(String.class);
      default:
        return ClassName.get(Class.forName(javaType));
    }
  }
}
