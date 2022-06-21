package gov.cms.model.dsl.codegen.plugin.transformer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.squareup.javapoet.CodeBlock;
import gov.cms.model.dsl.codegen.plugin.accessor.GrpcGetter;
import gov.cms.model.dsl.codegen.plugin.accessor.StandardSetter;
import gov.cms.model.dsl.codegen.plugin.model.ColumnBean;
import gov.cms.model.dsl.codegen.plugin.model.MappingBean;
import gov.cms.model.dsl.codegen.plugin.model.TransformationBean;
import org.junit.jupiter.api.Test;

/** Unit test for {@link IdHashFieldTransformer}. */
public class IdHashFieldTransformerTest {
  /** Verifies that required fields use {@code copyString}. */
  @Test
  public void testRequiredField() {
    ColumnBean column =
        ColumnBean.builder().name("mbi").nullable(true).sqlType("varchar(20)").build();
    TransformationBean transformation =
        TransformationBean.builder().optional(false).from("mbi").to("mbiHash").build();
    MappingBean mapping =
        MappingBean.builder()
            .entityClassName("gov.cms.test.Entity")
            .transformation(transformation)
            .build();

    var generator = new IdHashFieldTransformer();
    CodeBlock block =
        generator.generateCodeBlock(
            mapping, column, transformation, GrpcGetter.Instance, StandardSetter.Instance);
    assertEquals(
        "transformer.copyString(namePrefix + gov.cms.test.Entity.Fields.mbi, true, 1, 20, idHasher.apply(from.getMbi()), to::setMbi);\n",
        block.toString());
  }

  /** Verifies that optional fields use {@code copyOptionalString}. */
  @Test
  public void testOptionalField() {
    ColumnBean column =
        ColumnBean.builder().name("mbi").nullable(true).sqlType("varchar(20)").build();
    TransformationBean transformation =
        TransformationBean.builder().optional(true).from("mbi").to("mbiHash").build();
    MappingBean mapping =
        MappingBean.builder()
            .entityClassName("gov.cms.test.Entity")
            .transformation(transformation)
            .build();

    var generator = new IdHashFieldTransformer();
    CodeBlock block =
        generator.generateCodeBlock(
            mapping, column, transformation, GrpcGetter.Instance, StandardSetter.Instance);
    assertEquals(
        "transformer.copyOptionalString(namePrefix + gov.cms.test.Entity.Fields.mbi, 1, 20, from::hasMbi, ()-> idHasher.apply(from.getMbi()), to::setMbi);\n",
        block.toString());
  }
}
