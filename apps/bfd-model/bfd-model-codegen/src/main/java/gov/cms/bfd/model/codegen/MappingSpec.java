package gov.cms.bfd.model.codegen;

import com.fasterxml.jackson.databind.JsonNode;
import com.squareup.javapoet.ClassName;
import gov.cms.bfd.model.codegen.RifLayout.RifField;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.Transient;

/**
 * Encapsulates the information that must be known upfront to drive a mapping from a {@link
 * RifLayout} to Java code.
 *
 * <p>Each {@link RifLayout} will be mapped to either 1 or 2 JPA {@link Entity} classes: only 1 if
 * the record type doesn't have separate header vs. line fields, and exactly 2 if it does (one
 * {@link Entity} for the header fields and a second child {@link Entity} for the line fields).
 */
public final class MappingSpec {
  /*
   * Design Note: This class' fields are mutable to allow for the more
   * readable chained-setter style of construction.
   */

  private String packageName;
  private RifLayout rifLayout;
  private String headerEntity;
  private String headerTable;
  private String headerEntityIdField;
  private String headerEntityGeneratedIdField;
  private boolean hasLines = false;
  private boolean isBeneficiaryEntity = false;
  private String lineTable;
  private String lineEntityLineNumberField;
  private String sequenceNumberGeneratorName;
  private List<String> headerEntityTransientFields;
  private List<RifField> headerEntityAdditionalDatabaseFields;
  private List<InnerJoinRelationship> innerJoinRelationship;

  /**
   * Constructs a new {@link MappingSpec} instance.
   *
   * @param packageName the value to use for {@link #getPackageName()}
   */
  public MappingSpec(String packageName) {
    Objects.requireNonNull(packageName);
    this.packageName = packageName;
    this.headerEntityTransientFields = new ArrayList<>();
    this.headerEntityAdditionalDatabaseFields = new ArrayList<RifField>();
    this.innerJoinRelationship = new ArrayList<InnerJoinRelationship>();
  }

  public JsonNode createMetaData(MappingSummarizer logger) {
    var node = logger.createObject();
    node.put("type", "MappingSpec");
    node.put("packageName", packageName);
    node.set("rifLayout", rifLayout.createMetaData(logger));
    node.put("headerEntity", headerEntity);
    node.put("headerTable", headerTable);
    node.put("headerEntityIdField", headerEntityIdField);
    node.put("headerEntityGeneratedIdField", headerEntityGeneratedIdField);
    node.put("hasLines", hasLines);
    node.put("isBeneficiaryEntity", isBeneficiaryEntity);
    node.put("lineTable", lineTable);
    node.put("lineEntityLineNumberField", lineEntityLineNumberField);
    node.put("sequenceNumberGeneratorName", sequenceNumberGeneratorName);
    var array = logger.createArray();
    headerEntityTransientFields.forEach(array::add);
    node.set("headerEntityTransientFields", array);
    array = logger.createArray();
    headerEntityAdditionalDatabaseFields.stream()
        .map(rf -> rf.createMetaData(logger))
        .forEach(array::add);
    node.set("headerEntityAdditionalDatabaseFields", array);
    array = logger.createArray();
    innerJoinRelationship.stream().map(ijr -> ijr.createMetaData(logger)).forEach(array::add);
    node.set("innerJoinRelationship", array);
    return node;
  }

  /** @return the name of the Java package that the mapping is occurring for and in */
  public String getPackageName() {
    return packageName;
  }

  /** @return the {@link RifLayout} whose fields will be mapped */
  public RifLayout getRifLayout() {
    return rifLayout;
  }

  /**
   * @param rifLayout the new value for {@link #getRifLayout()}
   * @return the {@link MappingSpec} whose rifLayout is set
   */
  public MappingSpec setRifLayout(RifLayout rifLayout) {
    this.rifLayout = rifLayout;
    return this;
  }

  /**
   * @return the ClassName of the Java {@link Enum} that all the RIF field definitions will be
   *     placed in
   */
  public ClassName getColumnEnum() {
    return ClassName.get(packageName, headerEntity + "Column");
  }

  /**
   * @return the {@link ClassName} of the JPA {@link Entity} class that will be used to store data
   *     from this RIF layout for the header fields
   */
  public ClassName getHeaderEntity() {
    return ClassName.get(packageName, headerEntity);
  }

  /**
   * @param headerEntity the new value for {@link #getHeaderEntity()}
   * @return the {@link MappingSpec} whose headerEntity is set
   */
  public MappingSpec setHeaderEntity(String headerEntity) {
    this.headerEntity = headerEntity;
    return this;
  }

  /**
   * @return the name of the SQL table that the {@link #getHeaderEntity()} instances will be stored
   *     in
   */
  public String getHeaderTable() {
    return headerTable;
  }

  /**
   * Sets the header table.
   *
   * @param headerTable the new value for {@link #getHeaderTable()}
   * @return this {@link MappingSpec} instance, for call-chaining purposes
   */
  public MappingSpec setHeaderTable(String headerTable) {
    this.headerTable = headerTable;
    return this;
  }

  /**
   * @return the name of the {@link Entity} field that should be used as the {@link Id} in the
   *     {@link #getHeaderEntity()} {@link Entity}
   */
  public String getHeaderEntityIdField() {
    return headerEntityIdField;
  }

  /**
   * Sets the header entity id field.
   *
   * @param headerEntityIdField the new value for {@link #getHeaderEntityIdField()}
   * @return this {@link MappingSpec} instance, for call-chaining purposes
   */
  public MappingSpec setHeaderEntityIdField(String headerEntityIdField) {
    this.headerEntityIdField = headerEntityIdField;
    return this;
  }

  /**
   * @return the name of the {@link Entity} {@link GeneratedValue} field that should be used as the
   *     {@link Id} in the {@link #getHeaderEntity()} {@link Entity}
   */
  public String getHeaderEntityGeneratedIdField() {
    return headerEntityGeneratedIdField;
  }

  /**
   * @param headerEntityGeneratedIdField the new value for {@link
   *     #getHeaderEntityGeneratedIdField()}
   * @return this {@link MappingSpec} instance, for call-chaining purposes
   */
  public MappingSpec setHeaderEntityGeneratedIdField(String headerEntityGeneratedIdField) {
    this.headerEntityGeneratedIdField = headerEntityGeneratedIdField;
    return this;
  }

  /**
   * @return the name of the field in the {@link #getHeaderEntity()} {@link Entity} that should be
   *     used to store and refer to the child {@link #getLineEntity()} {@link Entity}s, if any
   */
  public String getHeaderEntityLinesField() {
    if (!hasLines) throw new IllegalStateException();
    return "lines";
  }

  /**
   * @return <code>true</code> if the RIF layout has child line fields that should be stored
   *     separately from its parent header fields, <code>false</code> if not
   */
  public boolean getHasLines() {
    return hasLines;
  }

  /**
   * @param hasLines the new value for {@link #getHasLines()}
   * @return the {@link MappingSpec} whose hasLines is set
   */
  public MappingSpec setHasLines(boolean hasLines) {
    this.hasLines = hasLines;
    return this;
  }

  /**
   * @return <code>true</code> if the RIF layout is for the <code>Beneficiary</code> entity, <code>
   *     false</code> if it's not
   */
  public boolean isBeneficiaryEntity() {
    return isBeneficiaryEntity;
  }

  /**
   * @param isBeneficiaryEntity the new value for {@link #isBeneficiaryEntity()}
   * @return this {@link MappingSpec}, for call chaining purposes
   */
  public MappingSpec setIsBeneficiaryEntity(boolean isBeneficiaryEntity) {
    this.isBeneficiaryEntity = isBeneficiaryEntity;
    return this;
  }

  /** @return the index of the last header field in {@link #getRifLayout()} */
  public int calculateLastHeaderFieldIndex() {
    return hasLines ? (calculateFirstLineFieldIndex() - 1) : (rifLayout.getRifFields().size() - 1);
  }

  /** @return the index of the first line field in {@link #getRifLayout()} */
  public int calculateFirstLineFieldIndex() {
    if (!hasLines) throw new IllegalStateException();

    for (int fieldIndex = 0; fieldIndex < rifLayout.getRifFields().size(); fieldIndex++) {
      RifField field = rifLayout.getRifFields().get(fieldIndex);
      if (field.getRifColumnName().equalsIgnoreCase(getLineEntityLineNumberField())) {
        return fieldIndex;
      }
    }

    throw new IllegalStateException();
  }

  /**
   * @return the name of the JPA {@link Entity} class that will be used to store data from this RIF
   *     layout for the line fields, if any
   */
  public ClassName getLineEntity() {
    if (!hasLines) throw new IllegalStateException();
    return ClassName.get(packageName, headerEntity + "Line");
  }

  /**
   * @return the name of the JPA {@link Entity} class that will be used to store data from this RIF
   *     layout for the line fields, if any
   */
  public ClassName getBeneficiaryMonthlyEntity() {
    if (!isBeneficiaryEntity) throw new IllegalStateException();
    return ClassName.get(packageName, "BeneficiaryMonthly");
  }

  /**
   * @return the name of the SQL table that the {@link #getLineEntity()} instances will be stored
   *     in, if any
   */
  public String getLineTable() {
    if (!hasLines) throw new IllegalStateException();
    return lineTable;
  }

  /**
   * Sets the line table.
   *
   * @param lineTable the new value for {@link #getLineTable()}
   * @return this {@link MappingSpec} instance, for call-chaining purposes
   */
  public MappingSpec setLineTable(String lineTable) {
    if (!hasLines) throw new IllegalStateException();
    this.lineTable = lineTable;
    return this;
  }

  /**
   * @return the ClassName of the JPA {@link IdClass} for the {@link #getLineEntity()} {@link
   *     Entity}, if any
   */
  public ClassName getLineEntityIdClass() {
    if (!hasLines) throw new IllegalStateException();
    return getLineEntity().nestedClass("LineId");
  }

  /**
   * @return the name of the field in the {@link #getLineEntity()} {@link Entity} that should be
   *     used to store and refer to the child {@link #getLineEntity()} {@link Entity}s, if any
   */
  public String getLineEntityParentField() {
    if (!hasLines) throw new IllegalStateException();
    return "parentClaim";
  }

  /**
   * @return the name of the field in the {@link #getBeneficiaryMonthlyEntityParentField()} {@link
   *     Entity} that should be used to store and refer to the parentBeneficiary {@link
   *     #getBeneficiaryMonthlyEntityParentField()} {@link Entity}s, if any
   */
  public String getBeneficiaryMonthlyEntityParentField() {
    if (!isBeneficiaryEntity) throw new IllegalStateException();
    return "parentBeneficiary";
  }

  /**
   * Sets the line entity line number field.
   *
   * @param lineEntityLineNumberField the name of the field in the {@link #getLineEntity()} {@link
   *     Entity} that should be used for the identifying line number
   * @return this {@link MappingSpec} instance, for call-chaining purposes
   */
  public MappingSpec setLineEntityLineNumberField(String lineEntityLineNumberField) {
    if (!hasLines) throw new IllegalStateException();
    this.lineEntityLineNumberField = lineEntityLineNumberField;
    return this;
  }

  /**
   * @return the name of the field in the {@link #getLineEntity()} {@link Entity} that should be
   *     used for the identifying line number, if any
   */
  public String getLineEntityLineNumberField() {
    // use Java field name since there is no uniformity among column names for line #
    if (!hasLines) throw new IllegalStateException();
    return lineEntityLineNumberField;
  }

  /**
   * @return the name of the field in the {@link #getEntityBeneficiaryMonthlyField()} {@link Entity}
   *     that should be used for the identifying yearMonth, if any
   */
  public String getEntityBeneficiaryMonthlyField() {
    if (!isBeneficiaryEntity) throw new IllegalStateException();
    return "YEAR_MONTH";
  }

  /** @return the fields in {@link #getHeaderEntity()} that should be marked as {@link Transient} */
  public List<String> getHeaderEntityTransientFields() {
    return headerEntityTransientFields;
  }

  /**
   * @param headerEntityTransientFields the new value for {@link #getHeaderEntityTransientFields()}
   * @return this {@link MappingSpec} instance, for call-chaining purposes
   */
  public MappingSpec setHeaderEntityTransientFields(String... headerEntityTransientFields) {
    Objects.requireNonNull(headerEntityTransientFields);
    this.headerEntityTransientFields = Arrays.asList(headerEntityTransientFields);
    return this;
  }

  /** @return the fields in {@link #getHeaderEntity()} that should be marked as {@link Transient} */
  public List<RifField> getHeaderEntityAdditionalDatabaseFields() {
    return headerEntityAdditionalDatabaseFields;
  }

  /**
   * @param headerEntityAdditionalDatabaseFields the new value for {@link
   *     #getHeaderEntityAdditionalDatabaseFields()}
   * @return this {@link MappingSpec} instance, for call-chaining purposes
   */
  public MappingSpec setHeaderEntityAdditionalDatabaseFields(
      List<RifField> headerEntityAdditionalDatabaseFields) {
    Objects.requireNonNull(headerEntityAdditionalDatabaseFields);
    this.headerEntityAdditionalDatabaseFields = headerEntityAdditionalDatabaseFields;
    return this;
  }

  /**
   * @param sequenceNumberGeneratorName the db sequence number generator name {@link
   *     #getSequenceNumberGeneratorName()}
   * @return this {@link MappingSpec} instance, for call-chaining purposes
   */
  public MappingSpec setSequenceNumberGeneratorName(String sequenceNumberGeneratorName) {
    this.sequenceNumberGeneratorName = sequenceNumberGeneratorName;
    return this;
  }

  /** @return this {@link MappingSpec} instance, for call-chaining purposes */
  public String getSequenceNumberGeneratorName() {
    return sequenceNumberGeneratorName;
  }

  /**
   * @return the {@link ClassName} for the class to be built that will contain parsing code for the
   *     layout
   */
  public ClassName getParserClass() {
    return ClassName.get(packageName, headerEntity + "Parser");
  }

  /**
   * @return the {@link ClassName} for the class to be built that will contain CSV writing for the
   *     layout
   */
  public ClassName getCsvWriterClass() {
    return ClassName.get(packageName, headerEntity + "CsvWriter");
  }

  /** @see java.lang.Object#toString() */
  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("MappingSpec [packageName=");
    builder.append(packageName);
    builder.append(", rifLayout=");
    builder.append(rifLayout);
    builder.append(", headerEntity=");
    builder.append(headerEntity);
    builder.append(", headerTable=");
    builder.append(headerTable);
    builder.append(", headerEntityIdField=");
    builder.append(headerEntityIdField);
    builder.append(", hasLines=");
    builder.append(hasLines);
    builder.append(", lineTable=");
    builder.append(lineTable);
    if (hasLines) {
      builder.append(", lineEntityLineNumber=");
      builder.append(lineEntityLineNumberField);
    }
    builder.append("]");
    return builder.toString();
  }

  /**
   * @return <code>true</code> if the RIF layout has an inner join relationship <code>false</code>
   *     if not
   */
  public Boolean getHasInnerJoinRelationship() {
    return this.innerJoinRelationship.size() != 0;
  }

  /**
   * @param innerJoinRelationship a list of {@link String} parameters defining the inner join
   *     relationship for the entity
   * @return this {@link MappingSpec} instance, for call-chaining purposes
   */
  public MappingSpec setInnerJoinRelationship(List<InnerJoinRelationship> innerJoinRelationship) {
    this.innerJoinRelationship = innerJoinRelationship;
    return this;
  }

  /** @return the list of {@link #innerJoinRelationship}s */
  public List<InnerJoinRelationship> getInnerJoinRelationship() {
    return this.innerJoinRelationship;
  }

  /**
   * @param entity the {@link String} entity for which to return the {@link ClassName} for
   * @return the {@link ClassName} of the provided {@link String} entity
   */
  public ClassName getClassName(String entity) {
    return ClassName.get(packageName, entity);
  }
}
