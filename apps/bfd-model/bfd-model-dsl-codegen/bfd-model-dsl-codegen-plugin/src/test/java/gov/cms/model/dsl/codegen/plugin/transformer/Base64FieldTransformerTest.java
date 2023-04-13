package gov.cms.model.dsl.codegen.plugin.transformer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.squareup.javapoet.CodeBlock;
import gov.cms.model.dsl.codegen.plugin.accessor.GrpcGetter;
import gov.cms.model.dsl.codegen.plugin.accessor.StandardSetter;
import gov.cms.model.dsl.codegen.plugin.model.ColumnBean;
import gov.cms.model.dsl.codegen.plugin.model.MappingBean;
import gov.cms.model.dsl.codegen.plugin.model.TransformationBean;
import org.junit.jupiter.api.Test;

/** Unit test for {@link Base64FieldTransformer}. */
class Base64FieldTransformerTest {
  /** Verifies that required fields use {@code copyBase64String}. */
  @Test
  void testRequiredField() {
    ColumnBean column =
        ColumnBean.builder().name("rdaClaimKey").nullable(false).sqlType("varchar(43)").build();
    TransformationBean transformation =
        TransformationBean.builder()
            .optionalComponents(TransformationBean.OptionalComponents.None)
            .transformerOption("decodedLength", "32")
            .from("rdaClaimKey")
            .build();
    MappingBean mapping =
        MappingBean.builder()
            .entityClassName("gov.cms.test.Entity")
            .transformation(transformation)
            .build();

    Base64FieldTransformer generator = new Base64FieldTransformer();
    CodeBlock block =
        generator.generateCodeBlock(
            mapping, column, transformation, GrpcGetter.Instance, StandardSetter.Instance);
    assertEquals(
        "transformer.copyBase64String(namePrefix + gov.cms.test.Entity.Fields.rdaClaimKey, false, 1, 43, 32, from.getRdaClaimKey(), to::setRdaClaimKey);\n",
        block.toString());
  }
}
