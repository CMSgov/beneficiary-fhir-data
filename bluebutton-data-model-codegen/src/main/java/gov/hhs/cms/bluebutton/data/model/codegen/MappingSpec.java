package gov.hhs.cms.bluebutton.data.model.codegen;

import java.util.Objects;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;

import com.squareup.javapoet.ClassName;

import gov.hhs.cms.bluebutton.data.model.codegen.RifLayout.RifField;

/**
 * <p>
 * Encapsulates the information that must be known upfront to drive a mapping
 * from a {@link RifLayout} to Java code.
 * </p>
 * <p>
 * Each {@link RifLayout} will be mapped to either 1 or 2 JPA {@link Entity}
 * classes: only 1 if the record type doesn't have separate header vs. line
 * fields, and exactly 2 if it does (one {@link Entity} for the header fields
 * and a second child {@link Entity} for the line fields).
 * </p>
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
	private boolean hasLines = false;
	private String lineTable;

	/**
	 * Constructs a new {@link MappingSpec} instance.
	 * 
	 * @param packageName
	 *            the value to use for {@link #getPackageName()}
	 */
	public MappingSpec(String packageName) {
		Objects.requireNonNull(packageName);
		this.packageName = packageName;
	}

	/**
	 * @return the name of the Java package that the mapping is occurring for
	 *         and in
	 */
	public String getPackageName() {
		return packageName;
	}

	/**
	 * @return the {@link RifLayout} whose fields will be mapped
	 */
	public RifLayout getRifLayout() {
		return rifLayout;
	}

	/**
	 * @param rifLayout
	 *            the new value for {@link #getRifLayout()}
	 */
	public MappingSpec setRifLayout(RifLayout rifLayout) {
		this.rifLayout = rifLayout;
		return this;
	}

	/**
	 * @return the ClassName of the Java {@link Enum} that all of the RIF field
	 *         definitions will be placed in
	 */
	public ClassName getColumnEnum() {
		return ClassName.get(packageName, headerEntity + "Column");
	}

	/**
	 * @return the {@link ClassName} of the JPA {@link Entity} class that will
	 *         be used to store data from this RIF layout for the header fields
	 */
	public ClassName getHeaderEntity() {
		return ClassName.get(packageName, headerEntity);
	}

	/**
	 * @param headerEntity
	 *            the new value for {@link #getHeaderEntity()}
	 */
	public MappingSpec setHeaderEntity(String headerEntity) {
		this.headerEntity = headerEntity;
		return this;
	}

	/**
	 * @return the name of the SQL table that the {@link #getHeaderEntity()}
	 *         instances will be stored in
	 */
	public String getHeaderTable() {
		return headerTable;
	}

	/**
	 * @param headerTable
	 *            the new value for {@link #getHeaderTable()}
	 */
	public MappingSpec setHeaderTable(String headerTable) {
		this.headerTable = headerTable;
		return this;
	}

	/**
	 * @return the name of the {@link Entity} field that should be used as the
	 *         {@link Id} in the {@link #getHeaderEntity()} {@link Entity}
	 */
	public String getHeaderEntityIdField() {
		return headerEntityIdField;
	}

	/**
	 * @param headerEntityIdField
	 *            the new value for {@link #getHeaderEntityIdField()}
	 */
	public MappingSpec setHeaderEntityIdField(String headerEntityIdField) {
		this.headerEntityIdField = headerEntityIdField;
		return this;
	}

	/**
	 * @return the name of the field in the {@link #getHeaderEntity()}
	 *         {@link Entity} that should be used to store and refer to the
	 *         child {@link #getLineEntity()} {@link Entity}s, if any
	 */
	public String getHeaderEntityLinesField() {
		if (!hasLines)
			throw new IllegalStateException();
		return "lines";
	}

	/**
	 * @return <code>true</code> if the RIF layout has child line fields that
	 *         should be stored separately from its parent header fields,
	 *         <code>false</code> if not
	 */
	public boolean getHasLines() {
		return hasLines;
	}

	/**
	 * @param hasLines
	 *            the new value for {@link #getHasLines()}
	 */
	public MappingSpec setHasLines(boolean hasLines) {
		this.hasLines = hasLines;
		return this;
	}

	/**
	 * @return the index of the last header field in {@link #getRifLayout()}
	 */
	public int calculateLastHeaderFieldIndex() {
		return hasLines ? (calculateFirstLineFieldIndex() - 1) : (rifLayout.getRifFields().size() - 1);
	}

	/**
	 * @return the index of the first line field in {@link #getRifLayout()}
	 */
	public int calculateFirstLineFieldIndex() {
		if (!hasLines)
			throw new IllegalStateException();

		for (int fieldIndex = 0; fieldIndex < rifLayout.getRifFields().size(); fieldIndex++) {
			RifField field = rifLayout.getRifFields().get(fieldIndex);
			if (field.getJavaFieldName().equals(getLineEntityLineNumberField()))
				return fieldIndex;
		}

		throw new IllegalStateException();
	}

	/**
	 * @return the name of the JPA {@link Entity} class that will be used to
	 *         store data from this RIF layout for the line fields, if any
	 */
	public ClassName getLineEntity() {
		if (!hasLines)
			throw new IllegalStateException();
		return ClassName.get(packageName, headerEntity + "Line");
	}

	/**
	 * @return the name of the SQL table that the {@link #getLineEntity()}
	 *         instances will be stored in, if any
	 */
	public String getLineTable() {
		if (!hasLines)
			throw new IllegalStateException();
		return lineTable;
	}

	/**
	 * @param lineTable
	 *            the new value for {@link #getLineTable()}
	 */
	public MappingSpec setLineTable(String lineTable) {
		if (!hasLines)
			throw new IllegalStateException();
		this.lineTable = lineTable;
		return this;
	}

	/**
	 * @return the ClassName of the JPA {@link IdClass} for the
	 *         {@link #getLineEntity()} {@link Entity}, if any
	 */
	public ClassName getLineEntityIdClass() {
		if (!hasLines)
			throw new IllegalStateException();
		return getLineEntity().nestedClass("LineId");
	}

	/**
	 * @return the name of the field in the {@link #getLineEntity()}
	 *         {@link Entity} that should be used to store and refer to the
	 *         child {@link #getLineEntity()} {@link Entity}s, if any
	 */
	public String getLineEntityParentField() {
		if (!hasLines)
			throw new IllegalStateException();
		return "parentClaim";
	}

	/**
	 * @return the name of the field in the {@link #getLineEntity()}
	 *         {@link Entity} that should be used for the identifying line
	 *         number, if any
	 */
	public String getLineEntityLineNumberField() {
		if (!hasLines)
			throw new IllegalStateException();
		return "lineNumber";
	}

	/**
	 * @return the {@link ClassName} for the class to be built that will contain
	 *         parsing code for the layout
	 */
	public ClassName getParserClass() {
		return ClassName.get(packageName, headerEntity + "Parser");
	}

	/**
	 * @return the {@link ClassName} for the class to be built that will contain
	 *         CSV writing for the layout
	 */
	public ClassName getCsvWriterClass() {
		return ClassName.get(packageName, headerEntity + "CsvWriter");
	}

	/**
	 * @see java.lang.Object#toString()
	 */
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
		builder.append("]");
		return builder.toString();
	}
}
