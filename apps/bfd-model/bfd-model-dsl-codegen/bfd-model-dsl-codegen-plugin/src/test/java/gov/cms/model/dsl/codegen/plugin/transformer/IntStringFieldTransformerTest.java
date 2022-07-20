package gov.cms.model.dsl.codegen.plugin.transformer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.squareup.javapoet.CodeBlock;
import gov.cms.model.dsl.codegen.plugin.accessor.GrpcGetter;
import gov.cms.model.dsl.codegen.plugin.accessor.StandardSetter;
import gov.cms.model.dsl.codegen.plugin.model.ColumnBean;
import gov.cms.model.dsl.codegen.plugin.model.MappingBean;
import gov.cms.model.dsl.codegen.plugin.model.TransformationBean;
import org.junit.jupiter.api.Test;

/** Unit test for {@link IntStringFieldTransformer}. */
public class IntStringFieldTransformerTest {
  /** Verifies that required fields use {@code copyIntString}. */
  @Test
  public void testRequiredField() {
    ColumnBean column =
        ColumnBean.builder().name("idrDtlCnt").nullable(true).sqlType("char(1)").build();
    TransformationBean transformation =
        TransformationBean.builder()
            .optionalComponents(TransformationBean.OptionalComponents.None)
            .from("idrDtlCnt")
            .build();
    MappingBean mapping =
        MappingBean.builder()
            .entityClassName("gov.cms.test.Entity")
            .transformation(transformation)
            .build();

    IntStringFieldTransformer generator = new IntStringFieldTransformer();
    CodeBlock block =
        generator.generateCodeBlock(
            mapping, column, transformation, GrpcGetter.Instance, StandardSetter.Instance);
    assertEquals(
        "transformer.copyIntString(namePrefix + gov.cms.test.Entity.Fields.idrDtlCnt, true, from.getIdrDtlCnt(), to::setIdrDtlCnt);\n",
        block.toString());
  }

  /** Verifies that optional fields use {@code copyOptionalIntString}. */
  @Test
  public void testOptionalField() {
    ColumnBean column =
        ColumnBean.builder().name("idrDtlCnt").nullable(true).sqlType("char(1)").build();
    TransformationBean transformation =
        TransformationBean.builder()
            .optionalComponents(TransformationBean.OptionalComponents.FieldAndProperty)
            .from("idrDtlCnt")
            .build();
    MappingBean mapping =
        MappingBean.builder()
            .entityClassName("gov.cms.test.Entity")
            .transformation(transformation)
            .build();

    IntStringFieldTransformer generator = new IntStringFieldTransformer();
    CodeBlock block =
        generator.generateCodeBlock(
            mapping, column, transformation, GrpcGetter.Instance, StandardSetter.Instance);
    assertEquals(
        "transformer.copyOptionalIntString(namePrefix + gov.cms.test.Entity.Fields.idrDtlCnt, from::hasIdrDtlCnt, from::getIdrDtlCnt, to::setIdrDtlCnt);\n",
        block.toString());
  }
}
