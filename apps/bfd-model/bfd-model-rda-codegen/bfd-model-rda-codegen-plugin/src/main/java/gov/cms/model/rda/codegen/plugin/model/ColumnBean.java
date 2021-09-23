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
  private String name;
  private String sqlType;
  private String javaType;
  private String enumType;
  private String comment;
  private boolean nullable = true;
  private boolean identity = false;

  public String getColumnName(String fieldName) {
    return Strings.isNullOrEmpty(name) ? fieldName : name;
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
      return mapJavaTypeToTypeName();
    }
  }

  public boolean hasComment() {
    return !Strings.isNullOrEmpty(comment);
  }

  public boolean isColumnDefRequired() {
    return sqlType.contains("decimal");
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

  public boolean isInt() {
    return "int".equals(javaType) || isIntType(mapSqlTypeToTypeName());
  }

  public boolean isDecimal() {
    return sqlType.contains("decimal");
  }

  public boolean isDate() {
    return sqlType.contains("date");
  }

  private boolean isStringType(TypeName type) {
    return (type instanceof ClassName) && ((ClassName) type).simpleName().equals("String");
  }

  private boolean isIntType(TypeName type) {
    return (type instanceof ClassName) && ((ClassName) type).simpleName().equals("Integer");
  }

  private TypeName mapSqlTypeToTypeName() {
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
    if (sqlType.contains("decimal")) {
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
  private TypeName mapJavaTypeToTypeName() {
    switch (javaType) {
      case "char":
        return TypeName.CHAR;
      case "int":
        return TypeName.INT;
      case "short":
        return TypeName.SHORT;
      case "long":
        return TypeName.LONG;
      default:
        return ClassName.get(Class.forName(javaType));
    }
  }
}
