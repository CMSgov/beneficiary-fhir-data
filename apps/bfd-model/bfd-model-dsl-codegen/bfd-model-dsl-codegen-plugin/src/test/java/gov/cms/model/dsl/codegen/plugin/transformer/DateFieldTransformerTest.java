package gov.cms.model.dsl.codegen.plugin.transformer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.squareup.javapoet.CodeBlock;
import gov.cms.model.dsl.codegen.plugin.accessor.GrpcGetter;
import gov.cms.model.dsl.codegen.plugin.accessor.StandardSetter;
import gov.cms.model.dsl.codegen.plugin.model.ColumnBean;
import gov.cms.model.dsl.codegen.plugin.model.MappingBean;
import gov.cms.model.dsl.codegen.plugin.model.TransformationBean;
import org.junit.jupiter.api.Test;

/** Unit test for {@link DateFieldTransformer}. */
public class DateFieldTransformerTest {
  /** Verifies that required fields use {@code copyDate}. */
  @Test
  public void testRequiredField() {
    ColumnBean column = ColumnBean.builder().name("beneDob").nullable(true).sqlType("date").build();
    TransformationBean transformation =
        TransformationBean.builder()
            .optionalComponents(TransformationBean.OptionalComponents.None)
            .from("beneDob")
            .build();
    MappingBean mapping =
        MappingBean.builder()
            .entityClassName("gov.cms.test.Entity")
            .transformation(transformation)
            .build();

    DateFieldTransformer generator = new DateFieldTransformer();
    CodeBlock block =
        generator.generateCodeBlock(
            mapping, column, transformation, GrpcGetter.Instance, StandardSetter.Instance);
    assertEquals(
        "transformer.copyDate(namePrefix + gov.cms.test.Entity.Fields.beneDob, true, from.getBeneDob(), to::setBeneDob);\n",
        block.toString());
  }

  /** Verifies that optional fields use {@code copyOptionalDate}. */
  @Test
  public void testOptionalField() {
    ColumnBean column = ColumnBean.builder().name("beneDob").nullable(true).sqlType("date").build();
    TransformationBean transformation = TransformationBean.builder().from("beneDob").build();
    MappingBean mapping =
        MappingBean.builder()
            .entityClassName("gov.cms.test.Entity")
            .transformation(transformation)
            .build();

    DateFieldTransformer generator = new DateFieldTransformer();
    CodeBlock block =
        generator.generateCodeBlock(
            mapping, column, transformation, GrpcGetter.Instance, StandardSetter.Instance);
    assertEquals(
        "transformer.copyOptionalDate(namePrefix + gov.cms.test.Entity.Fields.beneDob, from::hasBeneDob, from::getBeneDob, to::setBeneDob);\n",
        block.toString());
  }
}
