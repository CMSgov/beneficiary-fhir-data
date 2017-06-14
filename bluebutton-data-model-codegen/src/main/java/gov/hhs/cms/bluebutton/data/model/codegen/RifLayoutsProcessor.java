package gov.hhs.cms.bluebutton.data.model.codegen;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Table;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.StandardLocation;

import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.google.auto.service.AutoService;
import com.google.common.collect.ImmutableSet;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import gov.hhs.cms.bluebutton.data.model.codegen.RifLayout.RifColumnType;
import gov.hhs.cms.bluebutton.data.model.codegen.RifLayout.RifField;
import gov.hhs.cms.bluebutton.data.model.codegen.annotations.RifLayoutsGenerator;

/**
 * This <code>javac</code> annotation {@link Processor} reads in an Excel file
 * that details a RIF field layout, and then generates the Java code required to
 * work with that layout.
 */
@AutoService(Processor.class)
public final class RifLayoutsProcessor extends AbstractProcessor {
	/**
	 * The {@link ClassName#simpleName()} for {@link Entity}'s nested
	 * {@link IdClass}es.
	 */
	private static final String CLASS_NAME_CHILD_LINE_ID = "LineId";

	/**
	 * The {@link FieldSpec#name} for the field that defines the ordering of
	 * "line" {@link Entity}s.
	 */
	private static final String FIELD_NAME_CHILD_LINE_NUMBER = "number";

	/**
	 * The {@link FieldSpec#name} for the field that references the parent
	 * "grouped" {@link Entity} in child "line" {@link Entity}s.
	 */
	private static final String FIELD_NAME_PARENT_CLAIM = "parentClaim";

	/**
	 * Both Maven and Eclipse hide compiler messages, so setting this constant
	 * to <code>true</code> will also log messages out to a new source file.
	 */
	private static final boolean DEBUG = true;

	private final List<String> logMessages = new LinkedList<>();

	/**
	 * @see javax.annotation.processing.AbstractProcessor#getSupportedAnnotationTypes()
	 */
	@Override
	public Set<String> getSupportedAnnotationTypes() {
		return ImmutableSet.of(RifLayoutsGenerator.class.getName());
	}

	/**
	 * @see javax.annotation.processing.AbstractProcessor#getSupportedSourceVersion()
	 */
	@Override
	public SourceVersion getSupportedSourceVersion() {
		return SourceVersion.latestSupported();
	}

	/**
	 * @see javax.annotation.processing.AbstractProcessor#process(java.util.Set,
	 *      javax.annotation.processing.RoundEnvironment)
	 */
	@Override
	public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
		try {
			logNote("Processing triggered for '%s' on root elements '%s'.", annotations, roundEnv.getRootElements());

			Set<? extends Element> annotatedElements = roundEnv.getElementsAnnotatedWith(RifLayoutsGenerator.class);
			for (Element annotatedElement : annotatedElements) {
				if (annotatedElement.getKind() != ElementKind.PACKAGE)
					throw new RifLayoutProcessingException(annotatedElement,
							"The %s annotation is only valid on packages (i.e. in package-info.java).",
							RifLayoutsGenerator.class.getName());
				process((PackageElement) annotatedElement);
			}
		} catch (RifLayoutProcessingException e) {
			log(Diagnostic.Kind.ERROR, e.getMessage(), e.getElement());
		} catch (Exception e) {
			/*
			 * Don't allow exceptions of any type to propagate to the compiler.
			 * Log a warning and return, instead.
			 */
			StringWriter writer = new StringWriter();
			e.printStackTrace(new PrintWriter(writer));
			log(Diagnostic.Kind.ERROR, "FATAL ERROR: " + writer.toString());
		}

		if (roundEnv.processingOver())
			writeDebugLogMessages();

		return true;
	}

	/**
	 * @param annotatedPackage
	 *            the {@link PackageElement} to process that has been annotated
	 *            with {@link RifLayoutsGenerator}
	 * @throws IOException
	 *             An {@link IOException} may be thrown if errors are
	 *             encountered trying to generate source files.
	 */
	private void process(PackageElement annotatedPackage) throws IOException {
		RifLayoutsGenerator annotation = annotatedPackage.getAnnotation(RifLayoutsGenerator.class);
		logNote(annotatedPackage, "Processing package annotated with: '%s'.", annotation);

		/* Define the list of layouts that we expect to parse and generate. */
		List<LayoutType> layoutTypes = new LinkedList<>();
		layoutTypes.add(new LayoutType(annotation.beneficiarySheet(), "Beneficiary", "Beneficiaries", "beneficiaryId",
				Optional.empty()));
		layoutTypes.add(new LayoutType(annotation.carrierSheet(), "CarrierClaim", "CarrierClaims", "claimId",
				Optional.of("CarrierClaimLines")));

		/*
		 * Find the spreadsheet referenced by the annotation. It will define the
		 * RIF layouts.
		 */
		FileObject spreadsheetResource;
		try {
			spreadsheetResource = processingEnv.getFiler().getResource(StandardLocation.SOURCE_PATH,
					annotatedPackage.getQualifiedName().toString(), annotation.spreadsheetResource());
		} catch (IOException | IllegalArgumentException e) {
			throw new RifLayoutProcessingException(annotatedPackage,
					"Unable to find or open specified spreadsheet: '%s'.", annotation.spreadsheetResource());
		}
		logNote(annotatedPackage, "Found spreadsheet: '%s'.", annotation.spreadsheetResource());

		/* Parse the spreadsheet, extracting the layouts from it. */
		Map<LayoutType, RifLayout> layouts = new HashMap<>();
		Workbook spreadsheetWorkbook = null;
		try {
			spreadsheetWorkbook = new XSSFWorkbook(spreadsheetResource.openInputStream());

			for (LayoutType layoutType : layoutTypes)
				layouts.put(layoutType, RifLayout.parse(spreadsheetWorkbook, layoutType.getSheetName()));
		} finally {
			if (spreadsheetWorkbook != null)
				spreadsheetWorkbook.close();
		}
		logNote(annotatedPackage, "Parsed RIF layouts: '%s'.", layouts);

		/* Generate the code for each layout. */
		for (LayoutType layoutType : layoutTypes)
			generateCode(annotatedPackage, layoutType, layouts.get(layoutType));
	}

	/**
	 * Generates the code for the specified {@link RifLayout}.
	 * 
	 * @param packageTarget
	 *            the Java package to generate code into
	 * @param layoutType
	 *            the {@link LayoutType} of the layout to generate code for
	 * @param rifLayout
	 *            the {@link RifLayout} to generate code for
	 * @throws IOException
	 *             An {@link IOException} may be thrown if errors are
	 *             encountered trying to generate source files.
	 */
	private void generateCode(PackageElement packageTarget, LayoutType layoutType, RifLayout rifLayout)
			throws IOException {
		/*
		 * Determine if this RifLayout has separate claim lines, and if so where
		 * the two sections are divided.
		 */
		Optional<Integer> firstLineField = findFirstClaimLineField(rifLayout);
		int lastClaimGroupedField = firstLineField.isPresent() ? (firstLineField.get() - 1)
				: (rifLayout.getRifFields().size() - 1);

		/*
		 * First, create the Java enum for the RIF columns.
		 */

		TypeSpec.Builder columnsEnum = TypeSpec.enumBuilder(layoutType.getEntityName() + "Column")
				.addModifiers(Modifier.PUBLIC);
		for (int fieldIndex = 0; fieldIndex < rifLayout.getRifFields().size(); fieldIndex++) {
			RifField rifField = rifLayout.getRifFields().get(fieldIndex);
			columnsEnum.addEnumConstant(rifField.getRifColumnName());
		}

		JavaFile columnsEnumFile = JavaFile.builder(packageTarget.getQualifiedName().toString(), columnsEnum.build())
				.build();
		columnsEnumFile.writeTo(processingEnv.getFiler());

		/*
		 * Then, create the JPA Entity for the "line" fields, containing: fields
		 * and accessors.
		 */

		if (firstLineField.isPresent()) {
			ClassName lineClassName = ClassName.get(packageTarget.getQualifiedName().toString(),
					layoutType.getEntityName() + "Line");
			List<FieldSpec> lineFields = new LinkedList<>();
			List<MethodSpec> lineMethods = new LinkedList<>();

			// Create the @IdClass needed for the composite primary key.
			TypeSpec.Builder lineIdClass = TypeSpec.classBuilder(CLASS_NAME_CHILD_LINE_ID)
					.addSuperinterface(Serializable.class).addModifiers(Modifier.PUBLIC, Modifier.STATIC);
			lineIdClass.addField(
					FieldSpec.builder(long.class, "serialVersionUID", Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
							.initializer("$L", 1L).build());
			ClassName lineIdClassName = lineClassName.nestedClass(CLASS_NAME_CHILD_LINE_ID);

			// Add a field to that PK class for the parent claim's ID.
			RifField parentClaimRifField = rifLayout.getRifFields().stream()
					.filter(f -> layoutType.getEntityIdFieldName().equals(f.getJavaFieldName())).findAny().get();
			TypeName parentClaimIdFieldType = selectJavaFieldType(parentClaimRifField);
			FieldSpec.Builder parentIdField = FieldSpec.builder(parentClaimIdFieldType, FIELD_NAME_PARENT_CLAIM,
					Modifier.PRIVATE);
			lineIdClass.addField(parentIdField.build());
			MethodSpec.Builder parentGetter = MethodSpec.methodBuilder("getParentClaim")
					.addStatement("return $N", FIELD_NAME_PARENT_CLAIM).returns(parentClaimIdFieldType);
			lineIdClass.addMethod(parentGetter.build());

			// Add a field to that class for the line number.
			RifField rifLineNumberField = rifLayout.getRifFields().stream()
					.filter(f -> f.getJavaFieldName().equals(FIELD_NAME_CHILD_LINE_NUMBER)).findFirst().get();
			TypeName lineNumberFieldType = selectJavaFieldType(rifLineNumberField);
			FieldSpec.Builder lineNumberIdField = FieldSpec.builder(lineNumberFieldType, FIELD_NAME_CHILD_LINE_NUMBER,
					Modifier.PRIVATE);
			lineIdClass.addField(lineNumberIdField.build());
			MethodSpec.Builder lineNumberGetter = MethodSpec
					.methodBuilder("get" + capitalize(FIELD_NAME_CHILD_LINE_NUMBER))
					.addStatement("return $N", FIELD_NAME_CHILD_LINE_NUMBER).returns(lineNumberFieldType);
			lineIdClass.addMethod(lineNumberGetter.build());

			// Add hashCode() and equals(...) to the PK class.
			lineIdClass.addMethod(generateHashCodeMethod(parentIdField.build(), lineNumberIdField.build()));
			lineIdClass
					.addMethod(generateEqualsMethod(lineIdClassName, parentIdField.build(), lineNumberIdField.build()));

			// Add a field and accessor to the "line" Entity for the parent.
			ClassName parentClaimType = ClassName.get(packageTarget.getQualifiedName().toString(),
					layoutType.getEntityName());
			FieldSpec.Builder parentClaimField = FieldSpec.builder(parentClaimType, FIELD_NAME_PARENT_CLAIM,
					Modifier.PRIVATE);
			parentClaimField.addAnnotation(Id.class);
			parentClaimField
					.addAnnotation(AnnotationSpec.builder(ManyToOne.class).build());
			parentClaimField.addAnnotation(AnnotationSpec.builder(JoinColumn.class)
					.addMember("name", "$S", "`" + layoutType.entityIdFieldName + "`").build());
			lineFields.add(parentClaimField.build());
			MethodSpec.Builder parentClaimGetter = MethodSpec.methodBuilder("get" + capitalize(FIELD_NAME_PARENT_CLAIM))
					.addModifiers(Modifier.PUBLIC).addStatement("return $N", FIELD_NAME_PARENT_CLAIM)
					.returns(parentClaimType);
			lineMethods.add(parentClaimGetter.build());

			// For each "line" RIF field, create an Entity field with accessors.
			for (int fieldIndex = firstLineField.get(); fieldIndex < rifLayout.getRifFields().size(); fieldIndex++) {
				RifField rifField = rifLayout.getRifFields().get(fieldIndex);

				FieldSpec entityField = FieldSpec
						.builder(selectJavaFieldType(rifField), rifField.getJavaFieldName(), Modifier.PRIVATE)
						.addAnnotations(createAnnotations(layoutType, rifField)).build();
				MethodSpec.Builder entityGetter = MethodSpec.methodBuilder(calculateGetterName(entityField))
						.addModifiers(Modifier.PUBLIC).returns(selectJavaPropertyType(rifField));
				addGetterStatement(rifField, entityField, entityGetter);
				MethodSpec.Builder entitySetter = MethodSpec.methodBuilder(calculateSetterName(entityField))
						.addModifiers(Modifier.PUBLIC).returns(void.class)
						.addParameter(selectJavaPropertyType(rifField), entityField.name);
				addSetterStatement(rifField, entityField, entitySetter);

				lineFields.add(entityField);
				lineMethods.add(entityGetter.build());
				lineMethods.add(entitySetter.build());
			}

			// Create the Entity class.
			AnnotationSpec entityAnnotation = AnnotationSpec.builder(Entity.class).build();
			AnnotationSpec tableAnnotation = AnnotationSpec.builder(Table.class)
					.addMember("name", "$S", "`" + layoutType.getLineFieldsTableName().get() + "`").build();
			// TODO create rif-to-java parser method
			TypeSpec.Builder lineFieldsClass = TypeSpec.classBuilder(lineClassName).addAnnotation(entityAnnotation)
					.addAnnotation(AnnotationSpec.builder(IdClass.class).addMember("value", "$T.class", lineIdClassName)
							.build())
					.addAnnotation(tableAnnotation).addModifiers(Modifier.PUBLIC).addFields(lineFields);
			lineFieldsClass.addMethod(MethodSpec.constructorBuilder().addModifiers(Modifier.PUBLIC).build());
			lineFieldsClass
					.addMethod(MethodSpec.constructorBuilder().addModifiers(Modifier.PUBLIC)
							.addParameter(parentClaimType, FIELD_NAME_PARENT_CLAIM)
							.addParameter(lineNumberFieldType, FIELD_NAME_CHILD_LINE_NUMBER)
							.addStatement("this.$1N = $1N", FIELD_NAME_PARENT_CLAIM)
							.addStatement("this.$1N = $1N", FIELD_NAME_CHILD_LINE_NUMBER).build());
			lineFieldsClass.addMethods(lineMethods);
			lineFieldsClass.addType(lineIdClass.build());

			JavaFile lineFieldsClassFile = JavaFile
					.builder(packageTarget.getQualifiedName().toString(), lineFieldsClass.build()).build();
			lineFieldsClassFile.writeTo(processingEnv.getFiler());
		}

		/*
		 * Then, create the JPA Entity for the "grouped" fields, containing:
		 * fields, accessors, and a RIF-to-JPA-Entity parser.
		 */

		// Create an Entity field with accessors for each RIF field.
		List<FieldSpec> groupedFields = new LinkedList<>();
		List<MethodSpec> groupedMethods = new LinkedList<>();
		for (int fieldIndex = 0; fieldIndex <= lastClaimGroupedField; fieldIndex++) {
			RifField rifField = rifLayout.getRifFields().get(fieldIndex);

			FieldSpec entityField = FieldSpec
					.builder(selectJavaFieldType(rifField), rifField.getJavaFieldName(), Modifier.PRIVATE)
					.addAnnotations(createAnnotations(layoutType, rifField)).build();
			MethodSpec.Builder entityGetter = MethodSpec.methodBuilder(calculateGetterName(entityField))
					.addModifiers(Modifier.PUBLIC).returns(selectJavaPropertyType(rifField));
			addGetterStatement(rifField, entityField, entityGetter);
			MethodSpec.Builder entitySetter = MethodSpec.methodBuilder(calculateSetterName(entityField))
					.addModifiers(Modifier.PUBLIC).returns(void.class)
					.addParameter(selectJavaPropertyType(rifField), entityField.name);
			addSetterStatement(rifField, entityField, entitySetter);

			groupedFields.add(entityField);
			groupedMethods.add(entityGetter.build());
			groupedMethods.add(entitySetter.build());
		}

		// Add the parent-to-child join field and accessor, if appropriate.
		if (firstLineField.isPresent()) {
			ParameterizedTypeName childFieldType = ParameterizedTypeName.get(ClassName.get(List.class),
					ClassName.get(packageTarget.getQualifiedName().toString(), layoutType.getEntityName() + "Line"));
			FieldSpec.Builder childField = FieldSpec.builder(childFieldType, "lines", Modifier.PRIVATE)
					.initializer("new $T<>()", LinkedList.class);
			childField.addAnnotation(
					AnnotationSpec.builder(OneToMany.class).addMember("mappedBy", "$S", FIELD_NAME_PARENT_CLAIM)
							.addMember("orphanRemoval", "$L", true).addMember("fetch", "$T.EAGER", FetchType.class)
							.addMember("cascade", "$T.ALL", CascadeType.class).build());
			childField.addAnnotation(AnnotationSpec.builder(OrderBy.class)
					.addMember("value", "$S", FIELD_NAME_CHILD_LINE_NUMBER + " ASC").build());
			groupedFields.add(childField.build());
			MethodSpec.Builder childGetter = MethodSpec.methodBuilder("getLines").addModifiers(Modifier.PUBLIC)
					.addStatement("return $N", "lines").returns(childFieldType);
			groupedMethods.add(childGetter.build());
		}

		// Create the Entity class.
		AnnotationSpec entityAnnotation = AnnotationSpec.builder(Entity.class).build();
		AnnotationSpec tableAnnotation = AnnotationSpec.builder(Table.class)
				.addMember("name", "$S", "`" + layoutType.getGroupedFieldsTableName() + "`").build();
		TypeSpec.Builder groupedFieldsClass = TypeSpec.classBuilder(layoutType.getEntityName())
				.addAnnotation(entityAnnotation).addAnnotation(tableAnnotation).addModifiers(Modifier.PUBLIC)
				.addFields(groupedFields).addMethods(groupedMethods);

		JavaFile groupedFieldsClassFile = JavaFile
				.builder(packageTarget.getQualifiedName().toString(), groupedFieldsClass.build()).build();
		groupedFieldsClassFile.writeTo(processingEnv.getFiler());
	}

	/**
	 * @param fields
	 *            the fields that should be hashed
	 * @return a new <code>hashCode()</code> implementation that uses the
	 *         specified fields
	 */
	private static MethodSpec generateHashCodeMethod(FieldSpec... fields) {
		MethodSpec.Builder hashCodeMethod = MethodSpec.methodBuilder("hashCode").addAnnotation(Override.class)
				.addModifiers(Modifier.PUBLIC).returns(int.class).addStatement("return $T.hash($L)", Objects.class,
						Arrays.stream(fields).map(f -> f.name).collect(Collectors.joining(", ")));
		return hashCodeMethod.build();
	}

	/**
	 * @param typeName
	 *            the {@link TypeName} of the class to add this method for
	 * @param fields
	 *            the fields that should be compared
	 * @return a new <code>equals(...)</code> implementation that uses the
	 *         specified fields
	 */
	private static MethodSpec generateEqualsMethod(TypeName typeName, FieldSpec... fields) {
		MethodSpec.Builder hashCodeMethod = MethodSpec.methodBuilder("equals").addAnnotation(Override.class)
				.addModifiers(Modifier.PUBLIC).addParameter(Object.class, "obj").returns(boolean.class);

		hashCodeMethod.beginControlFlow("if (this == obj)").addStatement("return true").endControlFlow();
		hashCodeMethod.beginControlFlow("if (obj == null)").addStatement("return false").endControlFlow();
		hashCodeMethod.beginControlFlow("if (getClass() != obj.getClass())").addStatement("return false")
				.endControlFlow();
		hashCodeMethod.addStatement("$T other = ($T) obj", typeName, typeName);
		for (FieldSpec field : fields) {
			hashCodeMethod.beginControlFlow("if ($T.deepEquals($N, other.$N))", Objects.class, field, field)
					.addStatement("return false").endControlFlow();
		}
		hashCodeMethod.addStatement("return true");

		return hashCodeMethod.build();
	}

	/**
	 * @param rifLayout
	 *            the {@link RifLayout} to inspect
	 * @return the index of the first claim line {@link RifField} in the
	 *         specified {@link RifLayout}, or {@link Optional#empty()} if the
	 *         {@link RifLayout} doesn't have any claim lines
	 */
	private static Optional<Integer> findFirstClaimLineField(RifLayout rifLayout) {
		for (int fieldIndex = 0; fieldIndex < rifLayout.getRifFields().size(); fieldIndex++) {
			RifField field = rifLayout.getRifFields().get(fieldIndex);
			if (field.getJavaFieldName().equals(FIELD_NAME_CHILD_LINE_NUMBER))
				return Optional.of(fieldIndex);
		}

		return Optional.empty();
	}

	/**
	 * @param rifField
	 *            the {@link RifField} to select the corresponding Java type for
	 * @return the {@link TypeName} of the Java type that should be used to
	 *         represent the specified {@link RifField} in a JPA entity
	 */
	private static TypeName selectJavaFieldType(RifField rifField) {
		if (rifField.getRifColumnType() == RifColumnType.CHAR && rifField.getRifColumnLength() == 1
				&& !rifField.isRifColumnOptional())
			return TypeName.CHAR;
		else if (rifField.getRifColumnType() == RifColumnType.CHAR && rifField.getRifColumnLength() == 1
				&& rifField.isRifColumnOptional())
			return ClassName.get(Character.class);
		else if (rifField.getRifColumnType() == RifColumnType.CHAR)
			return ClassName.get(String.class);
		else if (rifField.getRifColumnType() == RifColumnType.DATE && rifField.getRifColumnLength() == 8)
			return ClassName.get(LocalDate.class);
		else if (rifField.getRifColumnType() == RifColumnType.NUM)
			return ClassName.get(BigDecimal.class);
		else
			throw new IllegalArgumentException("Unhandled field type: " + rifField);
	}

	/**
	 * @param rifField
	 *            the {@link RifField} to select the corresponding Java
	 *            getter/setter type for
	 * @return the {@link TypeName} of the Java type that should be used to
	 *         represent the specified {@link RifField}'s getter/setter in a JPA
	 *         entity
	 */
	private static TypeName selectJavaPropertyType(RifField rifField) {
		if (!rifField.isRifColumnOptional())
			return selectJavaFieldType(rifField);
		else
			return ParameterizedTypeName.get(ClassName.get(Optional.class), selectJavaFieldType(rifField));
	}

	/**
	 * @param layoutType
	 *            the {@link LayoutType} for the specified {@link RifField}
	 * @param rifField
	 *            the {@link RifField} to create the corresponding
	 *            {@link AnnotationSpec}s for
	 * @return an ordered {@link List} of {@link AnnotationSpec}s representing
	 *         the JPA, etc. annotations that should be applied to the specified
	 *         {@link RifField}
	 */
	private static List<AnnotationSpec> createAnnotations(LayoutType layoutType, RifField rifField) {
		LinkedList<AnnotationSpec> annotations = new LinkedList<>();

		// Add an @Id annotation, if appropriate.
		if (rifField.getJavaFieldName().equals(layoutType.getEntityIdFieldName())
				|| rifField.getJavaFieldName().equals(FIELD_NAME_CHILD_LINE_NUMBER)) {
			AnnotationSpec.Builder idAnnotation = AnnotationSpec.builder(Id.class);
			annotations.add(idAnnotation.build());
		}

		// Add an @Column annotation to every column.
		AnnotationSpec.Builder columnAnnotation = AnnotationSpec.builder(Column.class)
				.addMember("name", "$S", "`" + rifField.getJavaFieldName() + "`")
				.addMember("nullable", "$L", rifField.isRifColumnOptional());
		if (rifField.getRifColumnType() == RifColumnType.CHAR) {
			columnAnnotation.addMember("length", "$L", rifField.getRifColumnLength());
		} else if (rifField.getRifColumnType() == RifColumnType.NUM) {
			/*
			 * In SQL, the precision is the number of digits in the unscaled
			 * value, e.g. "123.45" has a precision of 5. The scale is the
			 * number of digits to the right of the decimal point, e.g. "123.45"
			 * has a scale of 2.
			 */
			// TODO verify that this scale works for everything
			int fixedScale = 2;
			columnAnnotation.addMember("precision", "$L", rifField.getRifColumnLength() + fixedScale);
			columnAnnotation.addMember("scale", "$L", fixedScale);
		}
		annotations.add(columnAnnotation.build());

		return annotations;
	}

	/**
	 * @param entityField
	 *            the JPA entity {@link FieldSpec} for the field that the
	 *            desired getter will wrap
	 * @return the name of the Java "getter" for the specified {@link FieldSpec}
	 */
	private static String calculateGetterName(FieldSpec entityField) {
		if (entityField.type.equals(TypeName.BOOLEAN) || entityField.type.equals(ClassName.get(Boolean.class)))
			return "is" + capitalize(entityField.name);
		else
			return "get" + capitalize(entityField.name);
	}

	/**
	 * @param rifField
	 *            the {@link RifField} to generate the "getter" statement for
	 * @param entityField
	 *            the {@link FieldSpec} for the field being wrapped by the
	 *            "getter"
	 * @param entityGetter
	 *            the "getter" method to generate the statement in
	 */
	private static void addGetterStatement(RifField rifField, FieldSpec entityField, MethodSpec.Builder entityGetter) {
		if (!rifField.isRifColumnOptional())
			entityGetter.addStatement("return $N", entityField);
		else
			entityGetter.addStatement("return $T.ofNullable($N)", Optional.class, entityField);
	}

	/**
	 * @param entityField
	 *            the JPA entity {@link FieldSpec} for the field that the
	 *            desired setter will wrap
	 * @return the name of the Java "setter" for the specified {@link FieldSpec}
	 */
	private static String calculateSetterName(FieldSpec entityField) {
		return "set" + capitalize(entityField.name);
	}

	/**
	 * @param rifField
	 *            the {@link RifField} to generate the "setter" statement for
	 * @param entityField
	 *            the {@link FieldSpec} for the field being wrapped by the
	 *            "setter"
	 * @param entitySetter
	 *            the "setter" method to generate the statement in
	 */
	private static void addSetterStatement(RifField rifField, FieldSpec entityField, MethodSpec.Builder entitySetter) {
		if (!rifField.isRifColumnOptional())
			entitySetter.addStatement("this.$N = $N", entityField, entityField);
		else
			entitySetter.addStatement("this.$N = $N.orElse(null)", entityField, entityField);
	}

	/**
	 * @param name
	 *            the {@link String} to capitalize the first letter of
	 * @return a capitalized {@link String}
	 */
	private static String capitalize(String name) {
		char first = name.charAt(0);
		return String.format("%s%s", Character.toUpperCase(first), name.substring(1));
	}

	/**
	 * Reports the specified log message.
	 * 
	 * @param logEntryKind
	 *            the {@link Diagnostic.Kind} of log entry to add
	 * @param associatedElement
	 *            the Java AST {@link Element} that the log entry should be
	 *            associated with, or <code>null</code>
	 * @param messageFormat
	 *            the log message format {@link String}
	 * @param messageArguments
	 *            the log message format arguments
	 */
	private void log(Diagnostic.Kind logEntryKind, Element associatedElement, String messageFormat,
			Object... messageArguments) {
		String logMessage = String.format(messageFormat, messageArguments);
		processingEnv.getMessager().printMessage(logEntryKind, logMessage, associatedElement);

		String logMessageFull;
		if (associatedElement != null)
			logMessageFull = String.format("[%s] at '%s': %s", logEntryKind, associatedElement, logMessage);
		else
			logMessageFull = String.format("[%s]: %s", logEntryKind, logMessage);
		logMessages.add(logMessageFull);
	}

	/**
	 * Reports the specified log message.
	 * 
	 * @param logEntryKind
	 *            the {@link Diagnostic.Kind} of log entry to add
	 * @param messageFormat
	 *            the log message format {@link String}
	 * @param messageArguments
	 *            the log message format arguments
	 */
	private void log(Diagnostic.Kind logEntryKind, String messageFormat, Object... messageArguments) {
		log(logEntryKind, null, messageFormat, messageArguments);
	}

	/**
	 * Reports the specified log message.
	 * 
	 * @param associatedElement
	 *            the Java AST {@link Element} that the log entry should be
	 *            associated with, or <code>null</code>
	 * @param messageFormat
	 *            the log message format {@link String}
	 * @param messageArguments
	 *            the log message format arguments
	 */
	private void logNote(Element associatedElement, String messageFormat, Object... messageArguments) {
		log(Diagnostic.Kind.NOTE, associatedElement, messageFormat, messageArguments);
	}

	/**
	 * Reports the specified log message.
	 * 
	 * @param associatedElement
	 *            the Java AST {@link Element} that the log entry should be
	 *            associated with, or <code>null</code>
	 * @param messageFormat
	 *            the log message format {@link String}
	 * @param messageArguments
	 *            the log message format arguments
	 */
	private void logNote(String messageFormat, Object... messageArguments) {
		log(Diagnostic.Kind.NOTE, null, messageFormat, messageArguments);
	}

	/**
	 * Writes out all of the messages in {@link #logMessages} to a log file in
	 * the annotation-generated source directory.
	 */
	private void writeDebugLogMessages() {
		if (!DEBUG)
			return;

		try {
			FileObject logResource = processingEnv.getFiler().createResource(StandardLocation.SOURCE_OUTPUT, "",
					"rif-layout-processor-log.txt");
			Writer logWriter = logResource.openWriter();
			for (String logMessage : logMessages) {
				logWriter.write(logMessage);
				logWriter.write('\n');
			}
			logWriter.flush();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	/**
	 * Models an expected RIF layout type.
	 */
	private static final class LayoutType {
		private final String sheetName;
		private final String entityName;
		private final String groupedFieldsTableName;
		private final String entityIdFieldName;
		private final String lineFieldsTableName;

		/**
		 * Constructs a new {@link LayoutType}.
		 * 
		 * @param sheetName
		 *            the value to use for {@link #getSheetName()}
		 * @param entityName
		 *            the value to use for {@link #getEntityName()}
		 * @param groupedFieldsTableName
		 *            the value to use for {@link #getGroupedFieldsTableName()}
		 * @param entityIdFieldName
		 *            the value to use for {@link #getEntityIdFieldName()}
		 * @param lineFieldsTableName
		 *            the value to use for {@link #getLineFieldsTableName()}
		 */
		public LayoutType(String sheetName, String entityName, String groupedFieldsTableName, String entityIdFieldName,
				Optional<String> lineFieldsTableName) {
			this.sheetName = sheetName;
			this.entityName = entityName;
			this.groupedFieldsTableName = groupedFieldsTableName;
			this.entityIdFieldName = entityIdFieldName;
			this.lineFieldsTableName = lineFieldsTableName.orElse(null);
		}

		/**
		 * @return the name of the Excel workbook sheet that the layout will be
		 *         defined in
		 */
		public String getSheetName() {
			return sheetName;
		}

		/**
		 * @return the name of the JPA {@link Entity} class that will be used to
		 *         store data from this RIF layout
		 */
		public String getEntityName() {
			return entityName;
		}

		/**
		 * @return the name of the SQL table that the {@link #getEntityName()}
		 *         instances will be stored in
		 */
		public String getGroupedFieldsTableName() {
			return groupedFieldsTableName;
		}

		/**
		 * @return the name of the JPA {@link Entity}'s field that should be
		 *         used as the {@link Id}
		 */
		public String getEntityIdFieldName() {
			return entityIdFieldName;
		}

		/**
		 * @return the name of the SQL table that the "line" {@link Entity}
		 *         instances will be stored in, if any
		 */
		public Optional<String> getLineFieldsTableName() {
			return Optional.ofNullable(lineFieldsTableName);
		}

		/**
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			return getSheetName();
		}
	}
}
