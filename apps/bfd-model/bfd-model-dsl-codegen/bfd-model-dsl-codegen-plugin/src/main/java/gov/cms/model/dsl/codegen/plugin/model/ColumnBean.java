package gov.cms.model.dsl.codegen.plugin.model;

import com.google.common.base.Strings;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.persistence.GenerationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Model object for columns in a SQL database mapping. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ColumnBean {
  /** Regex used to recognize numeric columns by their SQL type. */
  private static final Pattern NumericTypeRegex =
      Pattern.compile("(numeric|decimal)\\((\\d+)(,(\\d+))?\\)", Pattern.CASE_INSENSITIVE);
  /**
   * Group number in {@link ColumnBean#NumericTypeRegex} that contains the numeric precision value.
   */
  private static final int NumericPrecisionGroup = 2;
  /** Group number in {@link ColumnBean#NumericTypeRegex} that contains the numeric scale value. */
  private static final int NumericScaleGroup = 4;
  /**
   * Regex to extract the length from a SQL type if it is character type with a defined integer
   * length.
   */
  private static final Pattern CharacterLengthRegex =
      Pattern.compile("char\\((\\d+)\\)", Pattern.CASE_INSENSITIVE);
  /**
   * Group number in {@link ColumnBean#CharacterLengthRegex} that contains the numeric length value.
   */
  private static final int CharacterLengthGroup = 1;
  /** Regex used to recognize date columns by their SQL type. */
  private static final Pattern DateTypeRegex = Pattern.compile("date", Pattern.CASE_INSENSITIVE);

  /** Name of the field in the entity object corresponding to this column. */
  private String name;
  /** Alternative name used in database for this column. Defaults to {@link ColumnBean#name}. */
  private String dbName;
  /** SQL database type for this column. */
  private String sqlType;
  /** Specific java type for the field in the entity object corresponding to this column. */
  private String javaType;
  /**
   * Alternative type for generated accessor methods (getter/setter) for the field in the entity
   * object corresponding to this column.
   */
  private String javaAccessorType;
  /**
   * Name of an enum type defined in the same mapping as this column. Should be found by calling
   * {@link MappingBean#findEnum(String)} on the mapping.
   */
  private String enumType;
  /** Text for insertion into the generated entity as a javadoc comment on the field. */
  private String comment;
  /** Indicates whether the column in the database is nullable. */
  @Builder.Default private boolean nullable = true;
  /** Indicates whether the column in the database is an {@link GenerationType.IDENTITY} column. */
  @Builder.Default private boolean identity = false;
  /**
   * Indicates whether to add the updatable argument to the {@link javax.persistence.Column}
   * annotation.
   */
  @Builder.Default private boolean updatable = true;
  /**
   * The {@link FieldType} for the field. Either {@link FieldType.Column} or {@link
   * FieldType.Transient}.
   */
  @Builder.Default private FieldType fieldType = FieldType.Column;
  /**
   * Minimum allowed length for non-null string value. Negative value means no column specific
   * number has been set.
   */
  @Builder.Default private int minLength = -1;
  /** A {@link SequenceBean} if this column's value is set using a database sequence. */
  private SequenceBean sequence;

  /**
   * This enum is used to define the type of annotation to use for the field. RIF uses some {@link
   * javax.persistence.Transient} fields. Otherwise all are normal {@link
   * javax.persistence.Column}s.
   */
  public enum FieldType {
    /** Normal column field. */
    Column,
    /** Transient, non-column field. */
    Transient
  }

  /**
   * Gets the name of the database column. Uses {@link ColumnBean#dbName} if defined, otherwise uses
   * {@link ColumnBean#name}.
   *
   * @return name of the column in the database
   */
  public String getColumnName() {
    return Strings.isNullOrEmpty(dbName) ? name : dbName;
  }

  /**
   * Gets the value to use for minimum column length value in transformations. If no column specific
   * value has been set the provided default value is returned.
   *
   * @param defaultValue value to return if no column specific value has been set
   * @return minimum length for this column
   */
  public int computeMinLength(int defaultValue) {
    return minLength >= 0 ? minLength : defaultValue;
  }

  /**
   * Computes an appropriate length argument for the {@link javax.persistence.Column} annotation.
   *
   * @return zero if no length is needed, otherwise a valid length
   */
  public int computeLength() {
    if (Strings.isNullOrEmpty(sqlType)) {
      return 0;
    }
    Matcher matcher = CharacterLengthRegex.matcher(sqlType);
    if (matcher.find()) {
      return Integer.parseInt(matcher.group(CharacterLengthGroup));
    } else if (sqlType.equalsIgnoreCase("varchar(max)")) {
      return Integer.MAX_VALUE;
    } else {
      return 0;
    }
  }

  /**
   * Computes the appropriate java type for the field associated with this column.
   *
   * @return either the {@link ColumnBean#javaType} or a type derived from the {@link
   *     ColumnBean#sqlType}.
   */
  public TypeName computeJavaType() {
    if (Strings.isNullOrEmpty(javaType)) {
      return mapSqlTypeToTypeName();
    } else if (isEnum()) {
      throw new RuntimeException("cannot compute a type for an enum column on field " + name);
    } else {
      return mapJavaTypeToTypeName(javaType, name);
    }
  }

  /**
   * Computes the appropriate java type for the accessor (getter/setter) generated for the field
   * associated with this column.
   *
   * @return either the {@link ColumnBean#javaAccessorType} or the computed field type.
   */
  public TypeName computeJavaAccessorType() {
    if (Strings.isNullOrEmpty(javaAccessorType)) {
      return computeJavaType();
    } else {
      return mapJavaTypeToTypeName(javaAccessorType, name);
    }
  }

  /**
   * Determines if the field for this column has a defined java type for its accessor methods.
   *
   * @return true if the accessor methods use a defined type
   */
  public boolean hasDefinedAccessorType() {
    return !Strings.isNullOrEmpty(javaAccessorType);
  }

  /**
   * Determines if we have a javadoc comment for this field.
   *
   * @return true if we have a javadoc comment
   */
  public boolean hasComment() {
    return !Strings.isNullOrEmpty(comment);
  }

  /**
   * Computes an appropriate {@code precision} argument value for a {@link javax.persistence.Column}
   * annotation.
   *
   * @return 0 if none is needed, otherwise the value for the argument
   */
  public int computePrecision() {
    var matcher = NumericTypeRegex.matcher(sqlType);
    if (matcher.find()) {
      return Integer.parseInt(matcher.group(NumericPrecisionGroup));
    }
    return 0;
  }

  /**
   * Computes an appropriate {@code scale} argument value for a {@link javax.persistence.Column}
   * annotation.
   *
   * @return 0 if none is needed, otherwise the value for the argument
   */
  public int computeScale() {
    var matcher = NumericTypeRegex.matcher(sqlType);
    if (matcher.find()) {
      var value = matcher.group(NumericScaleGroup);
      if (!Strings.isNullOrEmpty(value)) {
        return Integer.parseInt(value);
      }
    }
    return 0;
  }

  /**
   * Determines if the field is an enum type.
   *
   * @return true if the field is an enum type
   */
  public boolean isEnum() {
    return !Strings.isNullOrEmpty(enumType);
  }

  /**
   * Determines if the field is a string type.
   *
   * @return true if the field is a string type
   */
  public boolean isString() {
    return isStringType(computeJavaType()) && isStringType(mapSqlTypeToTypeName());
  }

  /**
   * Determines if the field is a primitive char type.
   *
   * @return true if the field is a primitive char type
   */
  public boolean isChar() {
    return "char".equals(javaType);
  }

  /**
   * Determines if the field is a boxed char type.
   *
   * @return true if the field is a boxes char type
   */
  public boolean isCharacter() {
    return "Character".equals(javaType);
  }

  /**
   * Determines if the field is a primitive int type.
   *
   * @return true if the field is a primitive int type
   */
  public boolean isInt() {
    return "int".equals(javaType) || isIntType(mapSqlTypeToTypeName());
  }

  /**
   * Determines if the field is a primitive long type.
   *
   * @return true if the field is a primitive long type
   */
  public boolean isLong() {
    return "long".equals(javaType) || isLongType(mapSqlTypeToTypeName());
  }

  /**
   * Determines if the column is a numeric type.
   *
   * @return true if the column is a numeric type
   */
  public boolean isNumeric() {
    return NumericTypeRegex.matcher(sqlType).find();
  }

  /**
   * Determines if the column is a date type.
   *
   * @return true if the column is a date type
   */
  public boolean isDate() {
    return DateTypeRegex.matcher(sqlType).find();
  }

  /**
   * Determines if the column's value is set using a sequence.
   *
   * @return true if the column's value is set using a sequence
   */
  public boolean hasSequence() {
    return sequence != null;
  }

  /**
   * Determines an appropriate java type to use based on the value of our {@link
   * ColumnBean#sqlType}.
   *
   * @return an appropriate java type to use based on our {@link ColumnBean#sqlType}
   */
  private TypeName mapSqlTypeToTypeName() {
    final String sqlType = this.sqlType.toLowerCase();
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
    throw new RuntimeException("no mapping for sqlType " + sqlType + " and field " + name);
  }

  /**
   * Determines if the specified {@link TypeName} is a string.
   *
   * @param type type to check
   * @return true if the specified {@link TypeName} is a string
   */
  private static boolean isStringType(TypeName type) {
    return (type instanceof ClassName) && ((ClassName) type).simpleName().equals("String");
  }

  /**
   * Determines if the specified {@link TypeName} is a boxed int.
   *
   * @param type type to check
   * @return true if the specified {@link TypeName} is a boxed int
   */
  private static boolean isIntType(TypeName type) {
    return (type instanceof ClassName) && ((ClassName) type).simpleName().equals("Integer");
  }

  /**
   * Determines if the specified {@link TypeName} is a boxed long.
   *
   * @param type type to check
   * @return true if the specified {@link TypeName} is a boxed long
   */
  private static boolean isLongType(TypeName type) {
    return (type instanceof ClassName) && ((ClassName) type).simpleName().equals("Long");
  }

  /**
   * Compute the appropriate {@link TypeName} to use for the given {@code javaType}.
   *
   * @param javaType either {@link ColumnBean#javaType} or {@link ColumnBean#javaAccessorType}
   * @param name the {@link ColumnBean#name} for the {@link ColumnBean}
   * @return an appropriate {@link TypeName}
   */
  private static TypeName mapJavaTypeToTypeName(String javaType, String name) {
    return ModelUtil.mapJavaTypeToTypeName(javaType)
        .orElseThrow(
            () ->
                new RuntimeException(
                    "no java class exists for javaType " + javaType + " and field " + name));
  }
}
