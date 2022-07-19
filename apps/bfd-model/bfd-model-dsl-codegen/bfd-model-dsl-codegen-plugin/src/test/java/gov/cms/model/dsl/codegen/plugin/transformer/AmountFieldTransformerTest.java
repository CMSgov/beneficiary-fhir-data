package gov.cms.model.dsl.codegen.plugin.transformer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.squareup.javapoet.CodeBlock;
import gov.cms.model.dsl.codegen.plugin.accessor.GrpcGetter;
import gov.cms.model.dsl.codegen.plugin.accessor.StandardSetter;
import gov.cms.model.dsl.codegen.plugin.model.ColumnBean;
import gov.cms.model.dsl.codegen.plugin.model.MappingBean;
import gov.cms.model.dsl.codegen.plugin.model.TransformationBean;
import org.junit.jupiter.api.Test;

/** Unit test for {@link AmountFieldTransformer}. */
public class AmountFieldTransformerTest {
  /** Verifies that required fields use {@code copyAmount}. */
  @Test
  public void testRequiredField() {
    ColumnBean column =
        ColumnBean.builder().name("estAmtDue").nullable(true).sqlType("decimal(11,2)").build();
    TransformationBean transformation =
        TransformationBean.builder()
            .optionalComponents(TransformationBean.OptionalComponents.None)
            .from("estAmtDue")
            .build();
    MappingBean mapping =
        MappingBean.builder()
            .entityClassName("gov.cms.test.Entity")
            .transformation(transformation)
            .build();

    AmountFieldTransformer generator = new AmountFieldTransformer();
    CodeBlock block =
        generator.generateCodeBlock(
            mapping, column, transformation, GrpcGetter.Instance, StandardSetter.Instance);
    assertEquals(
        "transformer.copyAmount(namePrefix + gov.cms.test.Entity.Fields.estAmtDue, true, from.getEstAmtDue(), to::setEstAmtDue);\n",
        block.toString());
  }

  /** Verifies that optional fields use {@code copyOptionalAmount}. */
  @Test
  public void testOptionalField() {
    ColumnBean column =
        ColumnBean.builder().name("estAmtDue").nullable(true).sqlType("decimal(11,2)").build();
    TransformationBean field = TransformationBean.builder().from("estAmtDue").build();
    MappingBean mapping =
        MappingBean.builder().entityClassName("gov.cms.test.Entity").transformation(field).build();

    AmountFieldTransformer generator = new AmountFieldTransformer();
    CodeBlock block =
        generator.generateCodeBlock(
            mapping, column, field, GrpcGetter.Instance, StandardSetter.Instance);
    assertEquals(
        "transformer.copyOptionalAmount(namePrefix + gov.cms.test.Entity.Fields.estAmtDue, from::hasEstAmtDue, from::getEstAmtDue, to::setEstAmtDue);\n",
        block.toString());
  }
}
