package gov.cms.model.dsl.codegen.plugin.transformer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.squareup.javapoet.CodeBlock;
import gov.cms.model.dsl.codegen.plugin.accessor.GrpcGetter;
import gov.cms.model.dsl.codegen.plugin.accessor.StandardSetter;
import gov.cms.model.dsl.codegen.plugin.model.ColumnBean;
import gov.cms.model.dsl.codegen.plugin.model.MappingBean;
import gov.cms.model.dsl.codegen.plugin.model.TransformationBean;
import org.junit.jupiter.api.Test;

/** Unit test for {@link UintToShortFieldTransformer}. */
public class UintToShortFieldTransformerTest {
  /** Verifies that required fields use {@code copyUIntToShort}. */
  @Test
  public void testRequiredField() {
    ColumnBean column =
        ColumnBean.builder()
            .name("rdaPosition")
            .nullable(false)
            .javaType("int")
            .sqlType("smallint")
            .build();
    TransformationBean transformation =
        TransformationBean.builder()
            .optionalComponents(TransformationBean.OptionalComponents.None)
            .from("rdaPosition")
            .build();
    MappingBean mapping =
        MappingBean.builder()
            .entityClassName("gov.cms.test.Entity")
            .transformation(transformation)
            .build();

    UintToShortFieldTransformer generator = new UintToShortFieldTransformer();
    CodeBlock block =
        generator.generateCodeBlock(
            mapping, column, transformation, GrpcGetter.Instance, StandardSetter.Instance);
    assertEquals(
        "transformer.copyUIntToShort(namePrefix + gov.cms.test.Entity.Fields.rdaPosition, from.getRdaPosition(), to::setRdaPosition);\n",
        block.toString());
  }

  /** Verifies that optional fields throw an exception to indicate they are not supported. */
  @Test
  public void testOptionalField() {
    ColumnBean column =
        ColumnBean.builder()
            .name("rdaPosition")
            .nullable(true)
            .javaType("int")
            .sqlType("smallint")
            .build();
    TransformationBean transformation =
        TransformationBean.builder()
            .optionalComponents(TransformationBean.OptionalComponents.FieldAndProperty)
            .from("rdaPosition")
            .build();
    MappingBean mapping =
        MappingBean.builder()
            .entityClassName("gov.cms.test.Entity")
            .transformation(transformation)
            .build();

    UintToShortFieldTransformer generator = new UintToShortFieldTransformer();
    CodeBlock block =
        generator.generateCodeBlock(
            mapping, column, transformation, GrpcGetter.Instance, StandardSetter.Instance);
    assertEquals(
        "transformer.copyOptionalUIntToShort(namePrefix + gov.cms.test.Entity.Fields.rdaPosition, from::hasRdaPosition, from::getRdaPosition, to::setRdaPosition);\n",
        block.toString());
  }
}
