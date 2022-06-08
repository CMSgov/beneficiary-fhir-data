package gov.cms.model.dsl.codegen.plugin.transformer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import com.squareup.javapoet.CodeBlock;
import gov.cms.model.dsl.codegen.plugin.accessor.GrpcGetter;
import gov.cms.model.dsl.codegen.plugin.accessor.StandardSetter;
import gov.cms.model.dsl.codegen.plugin.model.ColumnBean;
import gov.cms.model.dsl.codegen.plugin.model.MappingBean;
import gov.cms.model.dsl.codegen.plugin.model.TransformationBean;
import org.junit.Test;

/** Unit test for {@link IntFieldTransformer}. */
public class IntFieldTransformerTest {
  /** Verifies that required fields throw an exception to indicate they are not supported. */
  @Test
  public void testRequiredField() {
    ColumnBean column =
        ColumnBean.builder().name("idrDtlCnt").nullable(true).sqlType("int").build();
    TransformationBean transformation =
        TransformationBean.builder().optional(false).from("idrDtlCnt").build();
    MappingBean mapping =
        MappingBean.builder()
            .entityClassName("gov.cms.test.Entity")
            .transformation(transformation)
            .build();

    IntFieldTransformer generator = new IntFieldTransformer();
    assertThrows(
        IllegalArgumentException.class,
        () ->
            generator.generateCodeBlock(
                mapping, column, transformation, GrpcGetter.Instance, StandardSetter.Instance));
  }

  /** Verifies that optional fields use {@code copyOptionalInt}. */
  @Test
  public void testOptionalField() {
    ColumnBean column =
        ColumnBean.builder().name("idrDtlCnt").nullable(true).sqlType("int").build();
    TransformationBean transformation = TransformationBean.builder().from("idrDtlCnt").build();
    MappingBean mapping =
        MappingBean.builder()
            .entityClassName("gov.cms.test.Entity")
            .transformation(transformation)
            .build();

    IntFieldTransformer generator = new IntFieldTransformer();
    CodeBlock block =
        generator.generateCodeBlock(
            mapping, column, transformation, GrpcGetter.Instance, StandardSetter.Instance);
    assertEquals(
        "transformer.copyOptionalInt(from::hasIdrDtlCnt, from::getIdrDtlCnt, to::setIdrDtlCnt);\n",
        block.toString());
  }
}
